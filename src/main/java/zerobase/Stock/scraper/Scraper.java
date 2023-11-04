package zerobase.Stock.scraper;

import zerobase.Stock.model.Company;
import zerobase.Stock.model.ScrapedResult;

public interface Scraper {
    Company scrapCompanyByTicker(String ticker);
    ScrapedResult scrap(Company company);
}
