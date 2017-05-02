package cathywu.datasecurity.aspect;

import cathywu.datasecurity.*;
import cathywu.datasecurity.exception.ResultCheckerRequiredException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
public class DataOperationAspect {

    private static final Logger logger = LoggerFactory.getLogger(DataOperationAspect.class);

    private DataSecurityConfig dataSecurityConfig;

    public DataOperationAspect(DataSecurityConfig dataSecurityConfig) {
        this.dataSecurityConfig = dataSecurityConfig;
    }

    @Pointcut("@annotation(cathywu.datasecurity.CheckAuthority)")
    public void validateMethod() {
    }

    @Around("validateMethod()")
    public Object validate(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("check method {}", joinPoint.toShortString());
        if (skipIfPossible(joinPoint)) return joinPoint.proceed();

        List<Pair<Parameter, Object>> paramArgPairs = getParamArgPairs(joinPoint);
        for (Pair<Parameter, Object> paramPair : paramArgPairs) {
            checkParameter(paramPair.getLeft(), paramPair.getRight());
        }
        Object returnValue = joinPoint.proceed();
        if (shouldCheckResult(joinPoint)) {
            checkResult(joinPoint, returnValue);
        }
        return returnValue;
    }

    private void checkResult(JoinPoint joinPoint, Object returnValue) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        ResultChecker annotation = method.getAnnotation(ResultChecker.class);
        if (annotation == null) {
            throw new ResultCheckerRequiredException("Method \"" + method.getName() + "\" should be annotated with @ResultChecker");
        }
        check(returnValue, annotation.value(), annotation.method());
    }

    private void check(Object returnValue, String checkerName, String checkerMethod) throws Throwable {
        DataChecker checker = dataSecurityConfig.getChecker(checkerName);
        if ("check".equals(checkerMethod)) {
            checker.check(returnValue);
        } else {
            checker.check(checkerMethod, returnValue);
        }
    }

    private boolean shouldCheckResult(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        CheckAuthority checkAuthorityAnnotation = method.getAnnotation(CheckAuthority.class);
        return checkAuthorityAnnotation.checkResult();
    }

    private boolean skipIfPossible(JoinPoint joinPoint) {
        if (joinPoint.getArgs() == null || joinPoint.getArgs().length == 0) {
            return true;
        }
        if (!(joinPoint.getSignature() instanceof MethodSignature)) {
            return true;
        }
        return dataSecurityConfig.canSkipSecurityCheck();
    }

    private List<Pair<Parameter, Object>> getParamArgPairs(JoinPoint joinPoint) {
        List<Pair<Parameter, Object>> pairs = new ArrayList<>();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] values = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (!parameter.isAnnotationPresent(ParamChecker.class)) {
                continue;
            }
            Pair<Parameter, Object> valuePair = Pair.of(parameter, values[i]);
            pairs.add(valuePair);
        }
        return pairs;
    }

    private void checkParameter(Parameter param, Object value) throws Throwable {
        ParamChecker paramChecker = param.getAnnotation(ParamChecker.class);
        check(value, paramChecker.value(), paramChecker.method());
    }
}
