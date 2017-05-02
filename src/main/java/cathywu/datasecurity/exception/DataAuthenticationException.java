package cathywu.datasecurity.exception;

import org.springframework.security.access.AccessDeniedException;

public class DataAuthenticationException extends AccessDeniedException {

    private static final long serialVersionUID = -6853995358290078701L;

    public DataAuthenticationException(String msg) {
        super(msg);
    }
}
