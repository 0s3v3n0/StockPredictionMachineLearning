package com.isaac.stock.predict;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.isaac.stock.model.RecurrentNets;
import com.isaac.stock.representation.model.*;
import com.isaac.stock.representation.PriceCategory;
import com.isaac.stock.representation.StockDataSetIterator;
import com.isaac.stock.utils.PlotUtil;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.*;



public class StockPricePrediction {

    private static final Logger log = LoggerFactory.getLogger(StockPricePrediction.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static int exampleLength = 22; // time series length, assume 22 working days per month

    private static final String SYMBOL = "GOOGL"; // TICKER SYMBOL TO SEARCH FOR

    private static final String INTERVAL = IntervalToReturn.ONE_DAY.value;

    private static final String X_RAPID_API_KEY = "XXX"; // FILL YOUR YAHOO API KEY HERE ( 500 requests/month for free )

    public static void main(String[] args) throws IOException {

        HttpResponse<String> response;
        List<TimeFrame> timeFrameList = new ArrayList<>();
        StockStream stockStream = new StockStream();
        var client = HttpClient.newHttpClient();
        //https://rapidapi.com/sparior/api/yahoo-finance15
        var uri = URI.create("https://yahoo-finance15.p.rapidapi.com/api/yahoo/hi/history/" + SYMBOL + "/" + INTERVAL);
        var request = HttpRequest
                .newBuilder()
                .uri(uri)
                .header("accept", "application/json")
                .header("X-RapidAPI-Key", X_RAPID_API_KEY)
                .header("X-RapidAPI-Host", "yahoo-finance15.p.rapidapi.com")
                .GET()
                .build();

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();

            // InputStream file = StockPricePrediction.class.getClassLoader().getResourceAsStream("data.json");
            Response res = objectMapper.readValue(json, Response.class);
            //MAPPING OF COMPLEX RESPONSE TO TIMEFRAME LIST
            Map<String, Map<String, String>> items = res.getItems();

            items.values().forEach(s -> timeFrameList.add(objectMapper.convertValue(s, TimeFrame.class)));

            stockStream.setMeta(res.getMeta());
            stockStream.setTimeFrameList(timeFrameList);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("RESPONSE STATUS: " + response.statusCode());
        log.info("RESPONSE BODY LENGTH:" + response.body().length());


        File file = new File("prices-split-adjusted.csv");
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for (int i = 0; i < stockStream.getTimeFrameList().size(); i++) {
            TimeFrame x = stockStream.getTimeFrameList().get(i);
            bw.write(x.getDate() + "," + SYMBOL + "," + x.getOpen() + "," + x.getClose() + "," + x.getLow() + "," + x.getHigh() + "," + x.getVolume());
            bw.newLine();
        }
        bw.close();

        int batchSize = 64; // mini-batch size
        double splitRatio = 0.9; // 90% for training, 10% for testing
        int epochs = 100; // training epochs

        log.info("Create dataSet iterator...");
        PriceCategory category = PriceCategory.ALL; // CLOSE: predict close price
        StockDataSetIterator iterator = new StockDataSetIterator("C:\\Users\\VII\\Desktop\\Work\\StockPrediction\\prices-split-adjusted.csv", SYMBOL, batchSize, exampleLength, splitRatio, category);
        log.info("Load test dataset...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();


        log.info("Build lstm networks...");
        MultiLayerNetwork net = RecurrentNets.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes()); //CREATE NEW NETWORK
        // UNCOMMENT LINE UNDER IF YOU WANT TO USE LAST SAVED NETWORK AND COMMENT UPPER LINE FOR CREATING NEW
        // MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(new File("src/main/resources/StockPriceLSTM_".concat(String.valueOf(category)).concat(".zip"))); // LOAD LAST SAVED NETWORK

        log.info("Training...");
        for (int i = 0; i < epochs; i++) {
            while (iterator.hasNext()) net.fit(iterator.next()); // fit model using mini-batch data
            iterator.reset(); // reset iterator
            net.rnnClearPreviousState(); // clear previous state
        }

        log.info("Saving model...");
        File locationToSave = new File("src/main/resources/StockPriceLSTM_".concat(String.valueOf(category)).concat(".zip"));
        // saveUpdater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this to train your network more in the future
        ModelSerializer.writeModel(net, locationToSave, true);

        log.info("Load model...");
        net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);

        log.info("Testing...");
        if (category.equals(PriceCategory.ALL)) {
            INDArray max = Nd4j.create(iterator.getMaxArray());
            INDArray min = Nd4j.create(iterator.getMinArray());
            predictAllCategories(net, test, max, min);
        } else {
            double max = iterator.getMaxNum(category);
            double min = iterator.getMinNum(category);
            predictPriceOneAhead(net, test, max, min, category);
        }
        log.info("Done...");
    }

    /**
     * Predict one feature of a stock one-day ahead
     */
    private static void predictPriceOneAhead(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, double max, double min, PriceCategory category) {
        double[] predicts = new double[testData.size()];
        double[] actuals = new double[testData.size()];
        for (int i = 0; i < testData.size(); i++) {
            predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getDouble(exampleLength - 1) * (max - min) + min;
            actuals[i] = testData.get(i).getValue().getDouble(0);
        }
        log.info("Print out Predictions and Actual Values...");
        log.info("Predict,Actual");
        for (int i = 0; i < predicts.length; i++) log.info(predicts[i] + "," + actuals[i]);
        log.info("Plot...");
        PlotUtil.plot(predicts, actuals, String.valueOf(category));
    }

    private static void predictPriceMultiple(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, double max, double min) {
        // TODO
    }

    /**
     * Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead
     */
    private static void predictAllCategories(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, INDArray max, INDArray min) {
        INDArray[] predicts = new INDArray[testData.size()];
        INDArray[] actuals = new INDArray[testData.size()];
        for (int i = 0; i < testData.size(); i++) {
            predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getRow(exampleLength - 1).mul(max.sub(min)).add(min);
            actuals[i] = testData.get(i).getValue();
        }
        log.info("Print out Predictions and Actual Values...");
        log.info("Predict\tActual");
        for (int i = 0; i < predicts.length; i++) log.info(predicts[i] + "\t" + actuals[i]);
        log.info("Plot...");
        for (int n = 0; n < 5; n++) {
            double[] pred = new double[predicts.length];
            double[] actu = new double[actuals.length];
            for (int i = 0; i < predicts.length; i++) {
                pred[i] = predicts[i].getDouble(n);
                actu[i] = actuals[i].getDouble(n);
            }
            String name;
            switch (n) {
                case 0:
                    name = "Stock OPEN Price";
                    break;
                case 1:
                    name = "Stock CLOSE Price";
                    break;
                case 2:
                    name = "Stock LOW Price";
                    break;
                case 3:
                    name = "Stock HIGH Price";
                    break;
                case 4:
                    name = "Stock VOLUME Amount";
                    break;
                default:
                    throw new NoSuchElementException();
            }
            PlotUtil.plot(pred, actu, name);
        }
    }

}
