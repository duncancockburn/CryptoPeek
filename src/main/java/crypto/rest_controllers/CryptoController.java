package crypto.rest_controllers;

import crypto.exceptions.APIUnavailableException;
import crypto.exceptions.ExchangeNotFoundException;
import crypto.model.CoinLIst.Coinslist;
import crypto.model.arbitrageModels.ArbitrageModel;
import crypto.model.cryptoCompareModels.CryptoAverage;
import crypto.model.cryptoCompareModels.CryptoModel;
import crypto.model.cryptoCompareModels.Exchanges;
import crypto.model.getCoinSnapshotByFullID.CoinSnapshotFullByIdMain;
import crypto.model.socialStatsModels.SocialStats;
import crypto.model.arbitrageModels.SymbolList;
import crypto.services.ArbitrageService;
import crypto.services.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
public class CryptoController {

    @Autowired
    CryptoService cryptoService;

    @Autowired
    ArbitrageService arbitrageService;

    @RequestMapping("/api")
    public CryptoModel getSnapShot(@RequestParam(value="fsym")String fsym, @RequestParam(value="tsym")String tsym)
            throws APIUnavailableException {
        System.out.println("requestmapping");
        return cryptoService.getCoinSnapshot(fsym, tsym);
    }

    @RequestMapping("/api/exchange")
    public Exchanges getSnapShot(
            @RequestParam(value="fsym")String fsym,
            @RequestParam(value="tsym")String tsym,
            @RequestParam(value="exch")String exchange)
            throws ExchangeNotFoundException {
        System.out.println("requestmapping");
        return cryptoService.getCoinSnapshotByExchange(fsym, tsym, exchange);
    }

    @RequestMapping("/api/exchange/highest")
    public Exchanges[] getCoinSnapshotByHighestExchange (@RequestParam(value="fsym")String fsym, @RequestParam(value="tsym")String tsym)
            throws ExchangeNotFoundException {
        return cryptoService.getCoinSnapshotByHighestAndLowestExchange(fsym, tsym);
    }

    @RequestMapping("/api/average")
    public CryptoAverage getCryptoAverage (@RequestParam(value="currency_1")String currency_1,
                                           @RequestParam(value="currency_2")String currency_2)throws ExchangeNotFoundException {
        return cryptoService.getAveragePrice(currency_1,currency_2);
    }

    @RequestMapping("/api/socialstats")
    public SocialStats getSocialStats(@RequestParam(value="id")int id)
            throws APIUnavailableException {
        return cryptoService.getSocialStats(id);
    }

    @RequestMapping("/api/snapshotfullbyid")
    public CoinSnapshotFullByIdMain getSnapShotFull(@RequestParam(value="id")int id)
            throws APIUnavailableException {
        return cryptoService.getCoinSnapshotFull(id);
    }

    @RequestMapping("/api/socialstatscoins")
    public ArrayList<SocialStats> getSocialStatsForFollowedCoins()
            throws APIUnavailableException {
        return cryptoService.getSocialStatsForFollowedCoins();
    }

    @RequestMapping("/api/test")
    public Coinslist getCoinsTest(){
        return cryptoService.coinsTest();
    }

    @RequestMapping("api/getTopArbitrage")
    public ArrayList<ArbitrageModel> getTopArb() {
        return arbitrageService.getTopArbitrageOps();
    }
}
