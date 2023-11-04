package zerobase.Stock.scheduler;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zerobase.Stock.model.Company;
import zerobase.Stock.model.ScrapedResult;
import zerobase.Stock.model.constants.CacheKey;
import zerobase.Stock.persist.CompanyRepository;
import zerobase.Stock.persist.DividendRepository;
import zerobase.Stock.persist.entity.CompanyEntity;
import zerobase.Stock.persist.entity.DividendEntity;
import zerobase.Stock.scraper.Scraper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    private final Scraper yahooFinanceScraper;

//    @Scheduled(fixedDelay = 1000)
//    public void test1() throws InterruptedException {
//        Thread.sleep(10000); //10초간 일시정지
//        System.out.println(Thread.currentThread().getName() + " 테스트 1 : " + LocalDateTime.now());
//    }
//
//    @Scheduled(fixedDelay = 1000)
//    public void test2() {
//        System.out.println(Thread.currentThread().getName() + " 테스트 2 : " + LocalDateTime.now());
//    }

    //일정 주기마다 수행
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
    @Scheduled(cron = "${scheduler.scrap.yahoo}") // 매일 0시 0분 0초에
            public void yahooFinanceScheduling() {
                log.info("scraping scheduler is started");
                // 저장된 회사 목록을 조회
                List<CompanyEntity> companies = this.companyRepository.findAll();

                // 회사마다 배당금 정보를 새로 스크래핑
                for (var company : companies) {
                    log.info("scraping scheduler is started -> " + company.getName()); // 로그를 잘 남기는 습관 ! 중요 !
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(
                    new Company(company.getTicker(), company.getName()));

            // 스크래핑한 배당금 정보 중 데이터베이스에 없는 값은 저장
            scrapedResult.getDividends().stream()
                    // 디비든 모델을 디비든 엔티티로 매핑
                    .map(e -> new DividendEntity(company.getId(), e))
                    // 엘리먼트를 하나씩 디비든 레파지토리에 삽입
                    .forEach(e -> {
                        boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if (!exists) {
                            this.dividendRepository.save(e);
                        }
                    });

            // 연속적으로 스크래핑 대상 사이트 서버에 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000); // 3 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }
}
