package com.project.userservice.scheduler;

import com.project.userservice.dto.request.PayrollRequest;
import com.project.userservice.entity.Employee;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.PayrollRepository;
import com.project.userservice.service.PayrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;

@Component
@ConditionalOnProperty(prefix = "app.scheduler.payroll", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PayrollScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayrollScheduler.class);
    
    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;
    private final PayrollService payrollService;
    private final StringRedisTemplate redisTemplate;

    public PayrollScheduler(EmployeeRepository employeeRepository, 
                            PayrollRepository payrollRepository,
                            PayrollService payrollService,
                            StringRedisTemplate redisTemplate) {
        this.employeeRepository = employeeRepository;
        this.payrollRepository = payrollRepository;
        this.payrollService = payrollService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Runs at 01:00 AM on the 1st day of every month.
     * Generates payroll for all active employees for the current month.
     */
    @Scheduled(cron = "${app.scheduler.payroll.cron:0 0 1 1 * ?}")
    public void generateMonthlyPayroll() {
        YearMonth currentMonth = YearMonth.now();
        String lockKey = "payroll:scheduler:" + currentMonth;
        String lockValue = java.util.UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey, lockValue, java.time.Duration.ofHours(2));
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("Payroll generation for {} is already running on another instance", currentMonth);
            return;
        }
        log.info("Starting automatic payroll generation...");
        try {
            LocalDate salaryMonth = currentMonth.atDay(1);
            String salaryMonthStr = currentMonth.toString();

            int page = 0;
            int size = 100;
            long generatedCount = 0;

            while (true) {
                Page<Employee> employeePage = employeeRepository.findByIsDeletedFalse(PageRequest.of(page, size));
                if (employeePage.isEmpty()) {
                    break;
                }

                for (Employee employee : employeePage.getContent()) {
                    if (employee.getStatus() != EmployeeStatus.ACTIVE) {
                        continue;
                    }

                    if (payrollRepository.existsByEmployeeAccountIdAndSalaryMonth(
                            employee.getAccountId(), salaryMonth)) {
                        continue;
                    }
                    if (employee.getBaseSalary() == null || employee.getBaseSalary().signum() <= 0) {
                        log.warn("Skipping employee {} because base salary is not configured",
                                employee.getAccountId());
                        continue;
                    }

                    try {
                        PayrollRequest request = new PayrollRequest(
                                employee.getAccountId(),
                                salaryMonthStr,
                                employee.getBaseSalary(),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                Collections.emptyList()
                        );
                        payrollService.create(request);
                        generatedCount++;
                    } catch (Exception e) {
                        log.error("Failed to generate payroll for employee ID: {}",
                                employee.getAccountId(), e);
                    }
                }

                page++;
            }

            log.info("Completed automatic payroll generation. Generated {} payroll records for month {}.",
                    generatedCount, salaryMonthStr);
        } finally {
            try {
                redisTemplate.execute(
                        new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                                        + "return redis.call('del', KEYS[1]) else return 0 end",
                                Long.class),
                        java.util.List.of(lockKey),
                        lockValue);
            } catch (RuntimeException exception) {
                log.warn("Unable to release payroll scheduler lock {}", lockKey, exception);
            }
        }
    }
}
