package crypto.services;

/**
 * Created by aaron on 8/10/17.
 */

import com.google.gson.Gson;
import crypto.exceptions.APIUnavailableException;
import crypto.exceptions.ExchangeNotFoundException;
import crypto.mappers.SocialStatsMapper;
import crypto.model.CoinLIst.Coins;
import crypto.model.cryptoCompareModels.CryptoAverage;
import crypto.model.cryptoCompareModels.CryptoModel;
import crypto.model.cryptoCompareModels.Exchanges;
import crypto.model.getcoinsnapshotbyfullID.CoinSnapshotFullByIdMain;
import crypto.model.socialStatsModels.SocialStats;
import crypto.model.socialStatsModels.SocialStatsCoins;
import crypto.model.socialStatsModels.SocialStatsForDbInsert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by aaron on 8/8/17.
 */
@Service
public class CryptoService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    SocialStatsMapper socialStatsMapper;

    @Cacheable("CryptoCache")
    public CryptoModel getCoinSnapshot(String fsym, String tsym) throws APIUnavailableException {
        String url = "https://www.cryptocompare.com/api/data/coinsnapshot/?fsym=" + fsym + "&tsym=" + tsym;
        CryptoModel cryptoModel;
        try {
            System.out.println("Cryptoservice running");
            cryptoModel = restTemplate.getForObject(url, CryptoModel.class);

            if (cryptoModel.getData().getExchanges().length < 1){
                throw new APIUnavailableException();
            }

        } catch (Exception e){
            throw new APIUnavailableException();
        }


        return cryptoModel;
    }

    public CoinSnapshotFullByIdMain getCoinSnapshotFull(int id) throws APIUnavailableException {
        String url = "https://www.cryptocompare.com/api/data/coinsnapshotfullbyid/?id=" + id;
        CoinSnapshotFullByIdMain coinSnapshotFullByIdMain;
        try {
            coinSnapshotFullByIdMain = restTemplate.getForObject(url, CoinSnapshotFullByIdMain.class);

            if (coinSnapshotFullByIdMain.getData().getStreamerDataRaw().length < 1){
                throw new APIUnavailableException();
            }
        } catch (Exception e){
            throw new APIUnavailableException();
        }

        return coinSnapshotFullByIdMain;
    }

    public SocialStats getSocialStats(int id) throws APIUnavailableException {
        String url = "https://www.cryptocompare.com/api/data/socialstats/?id=" + id;
        SocialStats socialStats;
        try {
            socialStats = restTemplate.getForObject(url, SocialStats.class);

            if (socialStats.getData().getCryptoCompare().getSimilarItems().length < 1){

                throw new APIUnavailableException();
            }
        } catch (Exception e){
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
    public void evictCryptoCache(){
        // we don't have to do anything in here - this method just needs to be called
    }

    public Exchanges getCoinSnapshotByExchange(String fsym, String tsym, String exchange) throws ExchangeNotFoundException {

        String url = "https://www.cryptocompare.com/api/data/coinsnapshot/?fsym=" + fsym + "&tsym=" + tsym;
        CryptoModel cryptoModel;
        try {
            System.out.println("Cryptoservice running");
            cryptoModel = restTemplate.getForObject(url, CryptoModel.class);

            if (cryptoModel.getData().getExchanges().length < 1){
                throw new APIUnavailableException();
            }

            // get all the exchanges into an array we can loop through
            Exchanges[] ex = cryptoModel.getData().getExchanges();

            for (Exchanges val : ex){
                if (val.getMarket().equalsIgnoreCase(exchange)){
                    return val;
                }
            }
            throw new ExchangeNotFoundException();
        } catch (Exception e){
            throw new ExchangeNotFoundException();
        }
    }

    public Exchanges[] getCoinSnapshotByHighestAndLowestExchange(String fsym, String tsym) throws ExchangeNotFoundException {

        String url = "https://www.cryptocompare.com/api/data/coinsnapshot/?fsym=" + fsym + "&tsym=" + tsym;
        CryptoModel cryptoModel;
        try {
            cryptoModel = restTemplate.getForObject(url, CryptoModel.class);

            if (cryptoModel.getData().getExchanges().length < 1){
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


            for (Exchanges val : ex){
                //this exchange higher than previous highest exchange return it
                if (Double.parseDouble(val.getPrice()) > Double.parseDouble(highestExchange.getPrice())){

                        highestExchange = val;
                }

            }
            result[0] = highestExchange;

            for (Exchanges val : ex){
                if (val.getMarket().equalsIgnoreCase("Cryptsy") || val.getMarket().equalsIgnoreCase("BTCE")
                        ||  val.getMarket().equalsIgnoreCase("Yobit")){
                    System.out.println("val = " + val.getMarket().toString());
                    continue;
                }
                //this exchange higher than previous highest exchange return it
                if (Double.parseDouble(val.getPrice()) < Double.parseDouble(lowestExchange.getPrice())){

                    lowestExchange = val;
                }

            }
            result[1] = lowestExchange;
            return result;

        } catch (Exception e){
            e.printStackTrace();
            throw new ExchangeNotFoundException();
        }
    }


    public CryptoAverage getAveragePrice(String currency_1, String currency_2) throws ExchangeNotFoundException {

        String url = "https://www.cryptocompare.com/api/data/coinsnapshot/?fsym=" + currency_1 + "&tsym=" + currency_2;
        // iterate through each exchange and sum up the price from each, after all exchanges - divide by total number of exchanges
        // them create a CryptoAverage object and set the currency_1, currency_2 and average price
        // then return that object to the controller which will return a view that displays that data
        CryptoAverage cryptoAverage = new CryptoAverage();
        try {
            CryptoModel cryptoModel= restTemplate.getForObject(url, CryptoModel.class);

            double sum = 0;
            int count = 0;
            for (int i=0; i< cryptoModel.getData().getExchanges().length; i++){
                System.out.println(Double.parseDouble(cryptoModel.getData().getExchanges()[i].getPrice()));
                if ((Double.parseDouble(cryptoModel.getData().getExchanges()[i].getPrice())) < ((sum /i)*.5)){
                    System.out.println("IN if - SUM = " + sum +"and sum/i is"+(sum/i));
                    count++;
                    continue;
                } else {
                    sum += Double.parseDouble(cryptoModel.getData().getExchanges()[i].getPrice());
                }

            }
            double n = sum / (cryptoModel.getData().getExchanges().length-count);
            cryptoAverage.setAvg_price(n);
            cryptoAverage.setFrom_currency(currency_1);
            cryptoAverage.setTo_currency(currency_2);
            return cryptoAverage;
        } catch (Exception e){
            e.printStackTrace();
            throw new ExchangeNotFoundException();
        }

    }

    public Coins coinsTest(){
        Gson gson = new Gson();
        try {
            String out = new Scanner(new URL("https://www.cryptocompare.com/api/data/coinlist/").openStream(), "UTF-8").useDelimiter("\\a").next();
            System.out.println(out);
            Coins coinsObject = gson.fromJson(out, Coins.class);
            return coinsObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}