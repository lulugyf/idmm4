package com.sitech.crmpd.idmm.broker.config;

import com.sitech.crmpd.idmm.cfg.QueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by guanyf on 6/13/2017.
 * 配置表数据加载
 */
@Configuration
public class Loader {

    private static final Logger log = LoggerFactory.getLogger(Loader.class);

    @Autowired
    private JdbcTemplate jdbcConfig; //配置库连接
    @Resource
    private Map<String, String> cfg_sqls;

    public List<QueueConfig> loadQueues() {
        List<QueueConfig> ql = new LinkedList<>();
        jdbcConfig.query(cfg_sqls.get("getQueues"),
                new RowCallbackHandler()
                {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        // select b.client_id, b.dest_topic_id, b.max_request, b.min_timeout,
                        //b.max_timeout,b.consume_speed_limit, b.max_messages, b.warn_messages, b.part_count
                        //from tc_topic_sub_{version} b
                        QueueConfig c = new QueueConfig();
                        int idx = 0;
                        c.setClientId(rs.getString(++idx).trim());
                        c.setDestTopicId(rs.getString(++idx).trim());
                        c.setMaxRequest(rs.getInt(++idx));
                        c.setMinTimeout(rs.getInt(++idx));
                        c.setMinTimeout(rs.getInt(++idx));
                        c.setConsumeSpeedLimit(rs.getInt(++idx));
                        c.setMaxMessages(rs.getInt(++idx));
                        c.setWarnMessages(rs.getInt(++idx));
                        c.setPartCount(rs.getInt(++idx));

                        ql.add(c);
                    }
                });
        log.info("load queues count: {}", ql.size());
        return ql;
    }
}
