package com.nhn.pinpoint.profiler.modifier.db.interceptor;

import com.nhn.pinpoint.common.ServiceType;
import com.nhn.pinpoint.profiler.context.Trace;
import com.nhn.pinpoint.profiler.context.TraceContext;
import com.nhn.pinpoint.profiler.interceptor.ByteCodeMethodDescriptorSupport;
import com.nhn.pinpoint.profiler.interceptor.MethodDescriptor;
import com.nhn.pinpoint.profiler.interceptor.SimpleAroundInterceptor;
import com.nhn.pinpoint.profiler.interceptor.TraceContextSupport;
import com.nhn.pinpoint.profiler.interceptor.util.JDBCScope;
import com.nhn.pinpoint.profiler.logging.PLoggerFactory;

import com.nhn.pinpoint.profiler.logging.PLogger;

/**
 * Datasource의 get을 추적해야 될것으로 예상됨.
 */
public class DataSourceGetConnectionInterceptor implements SimpleAroundInterceptor, ByteCodeMethodDescriptorSupport, TraceContextSupport {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();
//    private final MetaObject<Object> getUrl = new MetaObject<Object>("__getUrl");

    private MethodDescriptor descriptor;
    private TraceContext traceContext;

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        // 예외 케이스 : getConnection()에서 Driver.connect()가 호출되는지 알고 싶으므로 push만 한다.
        JDBCScope.push();


        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        trace.traceBlockBegin();
        trace.markBeforeTime();
    }

    @Override
    public void after(Object target, Object[] args, Object result) {
        if (isDebug) {
            // args에 암호가 있을 가능성이 있어서 로그에서 제외
            logger.afterInterceptor(target, null, result);
        }
        // 예외 케이스 : getConnection()에서 Driver.connect()가 호출되는지 알고 싶으므로 pop만 한다.
        JDBCScope.pop();


        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        try {
            trace.recordServiceType(ServiceType.DBCP);
            if (args == null) {
//                args == null인 경우 parameter가 없는 getConnection() 호출시
                trace.recordApi(descriptor, null);
            } else if(args.length == 2) {
//                args[1]은 패스워드라서 뺀다.
                trace.recordApi(descriptor, new Object[] {args[0]});
            }
            trace.recordException(result);

            trace.markAfterTime();
        } finally {
            trace.traceBlockEnd();
        }
    }

    @Override
    public void setMethodDescriptor(MethodDescriptor descriptor) {
        this.descriptor = descriptor;
        this.traceContext.cacheApi(descriptor);
    }

    @Override
    public void setTraceContext(TraceContext traceContext) {
        this.traceContext = traceContext;
    }
}
