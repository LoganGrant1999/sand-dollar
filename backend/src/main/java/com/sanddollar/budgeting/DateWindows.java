package com.sanddollar.budgeting;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class DateWindows {

    private static final ZoneId DENVER_TIMEZONE = ZoneId.of("America/Denver");

    public static class DateRange {
        private final LocalDate start;
        private final LocalDate end;

        public DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }

        public boolean contains(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end);
        }
    }

    public DateRange getCurrentMonthInDenver() {
        ZonedDateTime nowInDenver = ZonedDateTime.now(DENVER_TIMEZONE);
        YearMonth currentMonth = YearMonth.from(nowInDenver);

        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        return new DateRange(start, end);
    }

    public DateRange getMonthInDenver(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        return new DateRange(start, end);
    }

    public DateRange getLast90DaysInDenver() {
        ZonedDateTime nowInDenver = ZonedDateTime.now(DENVER_TIMEZONE);
        LocalDate today = nowInDenver.toLocalDate();
        LocalDate start = today.minus(90, ChronoUnit.DAYS);

        return new DateRange(start, today);
    }

    public DateRange getLastNMonthsInDenver(int months) {
        ZonedDateTime nowInDenver = ZonedDateTime.now(DENVER_TIMEZONE);
        YearMonth currentMonth = YearMonth.from(nowInDenver);
        YearMonth startMonth = currentMonth.minusMonths(months - 1);

        LocalDate start = startMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        return new DateRange(start, end);
    }

    public LocalDate getCurrentDateInDenver() {
        return ZonedDateTime.now(DENVER_TIMEZONE).toLocalDate();
    }

    public boolean isCurrentMonth(LocalDate date) {
        DateRange currentMonth = getCurrentMonthInDenver();
        return currentMonth.contains(date);
    }
}