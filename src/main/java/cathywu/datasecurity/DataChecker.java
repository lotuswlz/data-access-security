package cathywu.datasecurity;

import cathywu.datasecurity.exception.DataAuthenticationException;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DataChecker<T> {

    public abstract void check(T data) throws DataAuthenticationException;

    public abstract void check(Collection<T> data) throws DataAuthenticationException;

    /**
     * Notice: every method for checking data security should throw DataAuthenticationException if not pass.
     *
     * @param method
     * @param params
     * @throws DataAuthenticationException
     * @throws ReflectiveOperationException
     */
    public void check(String method, Object... params) throws Throwable {
        Class<?>[] classes = Stream.of(params)
                .map(Object::getClass)
                .collect(Collectors.toList())
                .toArray(new Class<?>[]{});
        try {
            this.getClass().getDeclaredMethod(method, classes).invoke(this, params);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
