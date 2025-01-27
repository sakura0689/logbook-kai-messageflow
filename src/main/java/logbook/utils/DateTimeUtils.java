package logbook.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 現在日時を YYYYMMDDHHmmssSSS 形式の文字列で取得します。
     * 
     * @return フォーマット済み日時文字列
     */
    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(FORMATTER);
    }
}