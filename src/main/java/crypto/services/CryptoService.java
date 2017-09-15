package crypto.services;

/**
 * Created by aaron on 8/10/17.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import crypto.exceptions.APIUnavailableException;
import crypto.exceptions.ExchangeNotFoundException;
import crypto.mappers.PersistData;
import crypto.mappers.TopCoinsMapper;
import crypto.model.cryptoCompareModels.CryptoAverage;
import crypto.model.cryptoCompareModels.CryptoModel;
import crypto.model.cryptoCompareModels.Exchanges;
import crypto.model.historicalModels.Coin;
import crypto.model.historicalModels.HistoMinute;
import crypto.model.historicalModels.PersistHistoMinute;
import crypto.model.miningContracts.MiningContracts;
import crypto.model.miningEquipment.MiningEquipment;
import crypto.model.topPairs.TopPairs;
import crypto.model.topCoins.TopCoins;
import crypto.mappers.SocialStatsMapper;
import crypto.model.getCoinSnapshotByFullID.CoinSnapshotFullByIdMain;
import crypto.model.socialStatsModels.SocialStats;
import crypto.model.socialStatsModels.SocialStatsCoins;
import crypto.model.socialStatsModels.SocialStatsForDbInsert;
import crypto.util.DateUnix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.net.URL;

import java.util.ArrayList;

/**
 * Created by aaron on 8/8/17.
 */
@Service
public class CryptoService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    TopCoinsMapper topCoinsMapper;

    @Autowired
    SocialStatsMapper socialStatsMapper;

    @Autowired
    PersistData persistData;

    @Autowired
    CryptoCompareService cryptoCompareService;



    @Cacheable("CryptoCache")
    public CryptoModel getCoinSnapshot(String fsym, String tsym) throws APIUnavailableException {
        String url = "https://www.cryptocompare.com/api/data/coinsnapshot/?fsym=" + fsym + "&tsym=" + tsym;
        CryptoModel cryptoModel;
        try {
            System.out.println("Cryptoservice running");
            cryptoModel = restTemplate.getForObject(url, CryptoModel.class);

            if (cryptoModel.getData().getExchanges().length < 1) {
                throw new APIUnavailableException();
            }

        } catch (Exception e) {
            throw new APIUnavailableException();
        }


        return cryptoModel;
    }

    public CoinSnapshotFullByIdMain getCoinSnapshotFull(int id) throws APIUnavailableException {
        String url = "https://www.cryptocompare.com/api/data/coinsnapshotfullbyid/?id=" + id;
        CoinSnapshotFullByIdMain coinSnapshotFullByIdMain;
        try {
            coinSnapshotFullByIdMain = restTemplate.getForObject(url, CoinSnapshotFullByIdMain.class);

            if (coinSnapshotFullByIdMain.getData().getStreamerDataRaw().length < 1) {
                throw new APIUnavailableException();
            }
        } catch (Exception e) {
            throw new APIUnavailableException();
        }

        return coinSnapshotFullByIdMain;
    }

    public SocialStats getSocialStats(int id) throws APIUnavailableException {
        String url = "https://www.cryptocompare.com/api/data/socialstats/?id=" + id;
        SocialStats socialStats;
        try {
            socialStats = restTemplate.getForObject(url, SocialStats.class);

            if (socialStats.getData().getCryptoCompare().getSimilarItems().length < 1) {

                throw new APIUnavailableException();
            }
        } catch (Exception e) {
            throw new APIUnavailableException();
        }

        return socialStats;
    }

    public ArrayList<SocialStats> getSocialStatsForFollowedCoins() throws APIUnavailableException {

        ArrayList<SocialStatsCoins> socialStatsCoinsArrayList = socialStatsMapper.getSocialStatsCoins();

        ArrayList<SocialStats> socialStatsArrayList = new ArrayList<>();


        for (int i = 0; i < socialStatsCoinsArrayList.size(); i++) {
            String url = "https://www.cryptocompare.com/api/data/socialstats/?id=" + socialStatsCoinsArrayList.get(i).getCoin_id();
            SocialStats socialStats;
            try {
                socialStats = restTemplate.getForObject(url, SocialStats.class);

                if (socialStats.getData().getCryptoCompare().getSimilarItems().length < 1) {

                    throw new APIUnavailableException();
                }
            } catch (Exception e) {
                throw new APIUnavailableException();
            }

            socialStatsArrayList.add(socialStats);

            SocialStatsForDbInsert socialStatsForDbInsert = new SocialStatsForDbInsert(socialStats);

            socialStatsMapper.addSocialStatsToDb(socialStatsForDbInsert);

        }

        return socialStatsArrayList;
    }

    // example of how to evict cache - it's just the annotation we need
    @CacheEvict("CryptoCache")
    public void evictCryptoCache() {
        // we don't have to do anything in here - this method just needs to be called
    }

    public Exchanges getCoinSnapshotByExchange(String fsym, String tsym, String exchange) throws ExchangeNotFoundException {

        String url = "https://www.cryptocompare.com/api/data/coinsnapshot/?fsym=" + fsym + "&tsym=" + tsym;
        CryptoModel cryptoModel;
        try {
            System.out.println("Cryptoservice running");
            cryptoModel = restTemplate.getForObject(url, CryptoModel.class);

            if (cryptoModel.getData().getExchanges().length < 1) {
                throw new APIUnavailableException();
            }

            // get all the exchanges into an array we can loop through
            Exchanges[] ex = cryptoModel.getData().getExchanges();

            for (Exchanges val : ex) {
                if (val.getMarket().equalsIgnoreCase(exchange)) {
                    return val;
                }
            }
            throw new ExchangeNotFoundException();
        } catch (Exception e) {
            throw new ExchangeNotFoundException();
        }
    }

    public Exchanges[] getCoinSnapshotByHighestAndLowestExchange(String fsym, String tsym) throws ExchangeNotFoundException {

        String url = "https://www.cryptocompare.com/api/data/coinsnapshot/?fsym=" + fsym + "&tsym=" + tsym;
        CryptoModel cryptoModel;
        try {
            cryptoModel = restTemplate.getForObject(url, CryptoModel.class);

            if (cryptoModel.getData().getExchanges().length < 1) {
                throw new APIUnavailableException();
            }

            // get all the exchanges into an array we can loop through
            Exchanges[] ex = cryptoModel.getData().getExchanges();
            Exchanges[] result = new Exchanges[2];

            Exchanges highestExchange = ex[0];
            Exchanges lowestExchange = ex[0];
            if (ex[0].getMarket().equalsIgnoreCase("BTCE") || ex[0].getMarket().equalsIgnoreCase("Cryptsy")
                    || ex[0].getMarket().equalsIgnoreCase("Yobit")) {
                lowestExchange = ex[1];
                highestExchange = ex[1];
            }


            for (Exchanges val : ex) {
                //this exchange higher than previous highest exchange return it
                if (Double.parseDouble(val.getPrice()) > Double.parseDouble(highestExchange.getPrice())) {

                    highestExchange = val;
                }

            }
            result[0] = highestExchange;

            for (Exchanges val : ex) {
                if (val.getMarket().equalsIgnoreCase("Cryptsy") || val.getMarket().equalsIgnoreCase("BTCE")
                        || val.getMarket().equalsIgnoreCase("Yobit")) {
                    System.out.println("val = " + val.getMarket().toString());
                    continue;
                }
                //this exchange higher than previous highest exchange return it
                if (Double.parseDouble(val.getPrice()) < Double.parseDouble(lowestExchange.getPrice())) {

                    lowestExchange = val;
                }

            }
            result[1] = lowestExchange;
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new ExchangeNotFoundException();
        }
    }

    public TopCoins[] getTop30() throws ExchangeNotFoundException {

        String url = "https://api.coinmarketcap.com/v1/ticker/?limit=30";

        ObjectMapper mapper = new ObjectMapper();
        try {
            TopCoins[] topCoins = mapper.readValue(new URL(url), TopCoins[].class);
            populateTop30ToDB(topCoins);
            return topCoins;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void populateTop30ToDB(TopCoins[] topCoins) {

        for (int i = 0; i < topCoins.length; i++) {
            topCoinsMapper.addNewTop(topCoins[i]);
        }

        return;
    }

    public CryptoAverage getAveragePrice(String currency_1, String currency_2) throws ExchangeNotFoundException {

        String url = "https://www.cryptocompare.com/api/data/coinsnapshot/?fsym=" + currency_1 + "&tsym=" + currency_2;
        // iterate through each exchange and sum up the price from each, after all exchanges - divide by total number of exchanges
        // them create a CryptoAverage object and set the currency_1, currency_2 and average price
        // then return that object to the controller which will return a view that displays that data
        CryptoAverage cryptoAverage = new CryptoAverage();
        try {
            CryptoModel cryptoModel = restTemplate.getForObject(url, CryptoModel.class);

            double sum = 0;
            int count = 0;
            for (int i = 0; i < cryptoModel.getData().getExchanges().length; i++) {
                System.out.println(Double.parseDouble(cryptoModel.getData().getExchanges()[i].getPrice()));
                if ((Double.parseDouble(cryptoModel.getData().getExchanges()[i].getPrice())) < ((sum / i) * .5)) {
                    System.out.println("IN if - SUM = " + sum + "and sum/i is" + (sum / i));
                    count++;
                    continue;
                } else {
                    sum += Double.parseDouble(cryptoModel.getData().getExchanges()[i].getPrice());
                }

            }
            double n = sum / (cryptoModel.getData().getExchanges().length - count);
            cryptoAverage.setAvg_price(n);
            cryptoAverage.setFrom_currency(currency_1);
            cryptoAverage.setTo_currency(currency_2);
            return cryptoAverage;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExchangeNotFoundException();
        }

    }

    public TopPairs getTopPairs(String fsym, String tsym, Integer limit, boolean sign)
            throws APIUnavailableException {

        String url = "https://min-api.cryptocompare.com/data/top/pairs?fsym=" + fsym + "&tsym=" + tsym +
                "&limit=" + limit + "&sign=" + sign;
        String url2 = "https://min-api.cryptocompare.com/data/top/pairs?fsym=" + fsym + "&tsym=" + tsym;

        //if user has specified limit and sign parameters, then use this call;
        //necessary since omitting the limit param results in error if using
        //call with all params included
        if (limit != null) {

            try {
                TopPairs topPairs = restTemplate.getForObject(url, TopPairs.class);
                return topPairs;
            } catch (Exception e) {
                throw new APIUnavailableException();
            }

            //if user has specified only fsym and tsym, then use this call
        } else {

            try {
                TopPairs topPairs = restTemplate.getForObject(url2, TopPairs.class);
                return topPairs;
            } catch (Exception e) {
                throw new APIUnavailableException();
            }
        }

    }


    public MiningContracts getMiningContracts() throws APIUnavailableException {
        String url = "https://www.cryptocompare.com/api/data/miningcontracts";

        try {
            MiningContracts miningContracts = restTemplate.getForObject(url, MiningContracts.class);
            return miningContracts;

        } catch (Exception e) {
            throw new APIUnavailableException();
        }
    }

    public MiningEquipment getMiningEquipment() throws APIUnavailableException {
        String url = "https://www.cryptocompare.com/api/data/miningequipment";

//        try {
        MiningEquipment miningEquipment = restTemplate.getForObject(url, MiningEquipment.class);
        return miningEquipment;

//        } catch (Exception e) {
//            throw new APIUnavailableException();
//        }
    }

    // Author: Nicola
    // Checks the DB to see if we have the last 5 minutes worth of raw data in the histo minute table.
    private boolean missing5Min() {
        long currentDate;
        long lastDate;
        String dateInDB;
        currentDate = DateUnix.currentTimeToSecs();
        dateInDB = persistData.getMostRecentTime();
        lastDate = Long.parseLong(persistData.getMostRecentTime());
        // Compares the current date to the last date in the table to see if they are less than 5 minutes apart.
        if (currentDate - lastDate < 300) {
            return false;
        } else return true;
    }

    // Author: Nicola
    // Checks the DB to see if we have the last 5 minutes worth of raw data for BTC in the histo minute table.
    private boolean missing5MinBTC() {
        long currentDate;
        long lastDate;
        String dateInDB;
        currentDate = DateUnix.currentTimeToSecs();
        dateInDB = persistData.getMostRecentTimeBTC();
        lastDate = Long.parseLong(persistData.getMostRecentTime());
        // Compares the current date to the last date in the table for BTC to see if they are less than 5 minutes apart.
        if (currentDate - lastDate < 300) {
            return false;
        } else return true;
    }

    // Author: Nicola
    public ArrayList<PersistHistoMinute> getHistoMinuteData() throws APIUnavailableException {

        ArrayList<PersistHistoMinute> responses = new ArrayList<>();

        Coin coin;
        // Checks to see if we are missing the last 5 minutes worth of data in the histo minute table.
        if (missing5Min()) {

            // Gets all of the ID and Symbols for all of the coins we are following except BTC from the DB.
            ArrayList<Coin> coinArrayList = persistData.getCoinFromDB();

            int i = 0;
            for (i = 0; i < coinArrayList.size(); i++) {
                coin = coinArrayList.get(i);

                HistoMinute histoMinute = new HistoMinute();

                //Calls the CryptoCompair API for each coin to get the raw histo minute data.
                try {
                    histoMinute = (HistoMinute) cryptoCompareService.callCryptoCompareAPI(
                            "https://min-api.cryptocompare.com/data/histominute?fsym=" + coin.getSymbol() + "&tsym=BTC&" +
                                    "limit=5&aggregate=1&e=CCCAGG", histoMinute);
                    if (histoMinute != null) {
                        PersistHistoMinute persistHistoMinute;
                        int x = 0;
                        for (x = 0; x < histoMinute.getData().length; x++) {
                            System.out.println(x);

                            persistHistoMinute = new PersistHistoMinute();

                            //Saves all of the data from the API call to the DB
                            persistHistoMinute.setClose(histoMinute.getData()[x].getClose());
                            persistHistoMinute.setHigh(histoMinute.getData()[x].getHigh());
                            persistHistoMinute.setLow(histoMinute.getData()[x].getLow());
                            persistHistoMinute.setOpen(histoMinute.getData()[x].getOpen());
                            persistHistoMinute.setTime(histoMinute.getData()[x].getTime());
                            persistHistoMinute.setVolumefrom(histoMinute.getData()[x].getVolumefrom());
                            persistHistoMinute.setVolumeto(histoMinute.getData()[x].getVolumeto());
                            persistHistoMinute.setCoinId(coin.getId());
                            responses.add(persistHistoMinute);

                            persistData.insertHistoMinuteData(persistHistoMinute);
                        }
                    }

                    throw new APIUnavailableException();
                } catch (APIUnavailableException e) {
                    e.toString();
                }

            }
        }
        return responses;
    }

    // Author: Nicola
    public ArrayList<PersistHistoMinute> getHistoMinuteDataBTC() throws APIUnavailableException {

        ArrayList<PersistHistoMinute> responses = new ArrayList<>();

        // Checks to see if we are missing the last 5 minutes worth of data in the histo minute table for BTC.
        if (missing5MinBTC()) {
            HistoMinute histoMinute = new HistoMinute();
            //Calls the CryptoCompair API for BTC to get the raw histo minute data.
            histoMinute = (HistoMinute) cryptoCompareService.callCryptoCompareAPI(
                    "https://min-api.cryptocompare.com/data/histominute?fsym=BTC&tsym=USD&" +
                            "limit=5&aggregate=1&e=CCCAGG", histoMinute);
           
            PersistHistoMinute persistHistoMinute;
            int x = 0;
            for (x = 0; x < histoMinute.getData().length; x++) {
                System.out.println(x);

                persistHistoMinute = new PersistHistoMinute();

                //Saves all of the data from the API call to the DB.
                persistHistoMinute.setClose(histoMinute.getData()[x].getClose());
                persistHistoMinute.setHigh(histoMinute.getData()[x].getHigh());
                persistHistoMinute.setLow(histoMinute.getData()[x].getLow());
                persistHistoMinute.setOpen(histoMinute.getData()[x].getOpen());
                persistHistoMinute.setTime(histoMinute.getData()[x].getTime());
                persistHistoMinute.setVolumefrom(histoMinute.getData()[x].getVolumefrom());
                persistHistoMinute.setVolumeto(histoMinute.getData()[x].getVolumeto());
                persistHistoMinute.setCoinId(1);
                persistHistoMinute.setCoinSymbol("BTC");
                responses.add(persistHistoMinute);

                persistData.insertHistoMinuteData(persistHistoMinute);
            }
        }return responses;
    }
}