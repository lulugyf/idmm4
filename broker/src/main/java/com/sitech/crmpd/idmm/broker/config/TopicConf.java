package com.sitech.crmpd.idmm.broker.config;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by guanyf on 6/28/2017.
 * 一套主题相关配置数据， 如果有更新的话， 就整体替换
 */
public class TopicConf {
    private static final Logger log = LoggerFactory.getLogger(TopicConf.class);


    private Map<String, Integer> pubs; // 发布关系配置, key: client_id + '@' + src_topic_id
    private Map<String, List<TopicMapping>> topicMapping; //主题映射配置数据
    private Map<String, List<String>> subscribes;        //目标主题订阅关系配置数据

    private static final LoadingCache<String, Pattern> MATCHERS = CacheBuilder
            .newBuilder().weakKeys().weakValues().expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Pattern>() {

                @Override
                public java.util.regex.Pattern load(String key) {
                    return java.util.regex.Pattern.compile(key);
                }

            });

    public static final Method PROPERTYOPTION_VALUEOF = ClassUtils.getMethod(PropertyOption.class,
            "valueOf", String.class);
    public static final ExpressionParser EXPRESSIONPARSER = new SpelExpressionParser();
    private static final LoadingCache<String, Expression> EXPRESSIONS = CacheBuilder.newBuilder()
            .weakKeys().weakValues().expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Expression>() {

                @Override
                public Expression load(String key) {
                    return EXPRESSIONPARSER.parseExpression(key);
                }

            });
    private static final LoadingCache<Long, StandardEvaluationContext> EVALUATION_CONTEXT = CacheBuilder
            .newBuilder().weakKeys().weakValues().expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, StandardEvaluationContext>() {

                @Override
                public StandardEvaluationContext load(Long key) throws Exception {
                    final StandardEvaluationContext context = new StandardEvaluationContext();
                    context.registerFunction("valueOf", PROPERTYOPTION_VALUEOF);
                    context.setVariable("P", PropertyOption.class);
                    return context;
                }
            });

    public void loadData(Loader l) {
        pubs = new HashMap<>();
        for(String[] x: l.loadPubs()) {
            pubs.put(x[0]+"@"+x[1], 1);
        }

        topicMapping = new HashMap<>();
        for(TopicMapping t: l.loadMapping()){
            List<TopicMapping> x1 = topicMapping.getOrDefault(t.getSourceTopicId(), new LinkedList<>());
            if(x1.size() == 0) topicMapping.put(t.getSourceTopicId(), x1);
            x1.add(t);
        }

        subscribes = new HashMap<>();
        for(String[] x: l.loadSubs()){
            String clientId = x[0];
            String targetTopicId = x[1];
            List<String> x1 = subscribes.getOrDefault(targetTopicId, new LinkedList<>());
            if(x1.size() == 0) subscribes.put(targetTopicId, x1);
            x1.add(clientId);
        }
    }
    /**
     * 检查发布关系是否允许
     * @param clientId
     * @param srcTopicId
     * @return
     */
    public boolean checkPub(String clientId, String srcTopicId) {
        return pubs.containsKey(clientId + "@" + srcTopicId);
    }

    /**
     * 计算主题映射
     * @param srcTopicId
     * @param msg
     * @return 返回匹配到的目标主题+消费者 列表
     */
    public List<String[]> getMapping(String srcTopicId, Message msg) {
        if(!topicMapping.containsKey(srcTopicId))
            return null;
        List<String[]> ret = new LinkedList<>();
        for(TopicMapping m: topicMapping.get(srcTopicId)){
            final String propertyKey = m.getPropertyKey();
            final String propertyValue = m.getPropertyValue();
            if("_ignore".equals(m.getTargetTopicId()) )
                continue;
            if("_default".equals(propertyValue)){
                for(String clientid: subscribes.get(m.getTargetTopicId()))
                    ret.add(new String[]{m.getTargetTopicId(), clientid});
                continue;
            }
            if(!Strings.isNullOrEmpty(propertyKey)) {
                // 简单匹配
                final String value = msg.existProperty(propertyKey) ? msg.getProperty(
                        propertyKey).toString() : msg.getStringProperty(PropertyOption
                        .valueOf(propertyKey));
                // 属性值可以是“_default”，表示如果生产者没有传递这个属性信息或者是定义的各个属性值都不符合要求
                if (value == null) {
                    log.trace("未取到值不匹配");
                    continue;
                }

                // 值匹配不成功
                if (MATCHERS.getUnchecked(propertyValue).matcher(value).matches()) {
                    for(String clientid: subscribes.get(m.getTargetTopicId()))
                        ret.add(new String[]{m.getTargetTopicId(), clientid});
                    continue;
                }
            }
        }
        return ret;
    }

}
