package cathywu.datasecurity;

public interface DataSecurityConfig {

    boolean canSkipSecurityCheck();

    DataChecker getChecker(String name);
}
