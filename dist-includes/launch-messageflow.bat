REM UTF-8対応
chcp 65001 >nul

SET JVM_OPT=-XX:MaxMetaspaceSize=256M

REM アプリケーションを起動
START javaw %JVM_OPT% -Dport=8890 -Dkaiport=8888 -jar logbook-kai-messageflow.jar
