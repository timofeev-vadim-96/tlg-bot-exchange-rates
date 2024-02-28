package org.example.util;

import java.time.LocalDate;

public class DateManager {
    public enum TimeUnit {
        YEAR, MONTH, WEEK, DAY
    }

    /**
     * Метод создания даты в прошлом
     * @param timeUnit единица времени
     * @param units значение
     * @return дату в прошлом
     */
    public static LocalDate getDateFromThePast(TimeUnit timeUnit, short units){
        LocalDate localDate = LocalDate.now();
        switch (timeUnit){
            case YEAR -> {
                return localDate.minusYears(units);
            }
            case MONTH -> {
                return localDate.minusMonths(units);
            }
            case WEEK -> {
                return localDate.minusWeeks(units);
            }
            case DAY -> {
                return localDate.minusDays(units);
            }
            default -> throw new IllegalArgumentException("Unknown timeUnit");
        }
    }
}
