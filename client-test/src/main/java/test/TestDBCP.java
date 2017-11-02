package test;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.sql.Connection;

/**
 * Created by gyf on 9/20/2017.
 *
 *
 <bean id="dsProperties" class="org.apache.tomcat.jdbc.pool.PoolProperties">
 <property name="driverClassName" value="${jdbc.driverClassName}" />
 <property name="url" value="${jdbc.url}" />
 <property name="username" value="${jdbc.username}" />
 <property name="password" value="${jdbc.password}" />
 <property name="maxActive" value="${jdbc.maxActive}" />
 <property name="initialSize" value="${jdbc.initialSize}" />
 <property name="testWhileIdle" value="true" />
 <property name="validationQuery" value="select 1 from dual" />

 <property name="jmxEnabled" value="true" />
 <property name="minIdle" value="${jdbc.initialSize}" />
 <property name="logAbandoned" value="true" />
 <property name="removeAbandoned" value="true" />
 </bean>
 */
public class TestDBCP {
    public static void main(String[] args) throws Exception {
        PoolProperties p = new PoolProperties();
        p.setUrl("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.110.26.239)(PORT=1821))(ADDRESS=(PROTOCOL=TCP)(HOST=10.110.26.239)(PORT=1821)))(LOAD_BALANCE=OFF)(FAILOVER=ON)(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=tmanadb))(FAILOVER_MODE=(TYPE=session)(METHOD=basic)(RETRIES=180)(DELAY=5)))");
        p.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        p.setUsername("dbidmmopr");
        p.setPassword("Support1");
        p.setJmxEnabled(true); //
        p.setTestWhileIdle(false); //
        p.setTestOnBorrow(true);
        p.setValidationQuery("SELECT 1 FROM DUAL"); //
        p.setTestOnReturn(false); //
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMaxActive(500); //
        p.setInitialSize(20); //
        p.setMaxWait(10000);
        p.setRemoveAbandonedTimeout(60);
        p.setMinEvictableIdleTimeMillis(30000);
        p.setMinIdle(20);
        p.setLogAbandoned(true); //
        p.setRemoveAbandoned(true);//

        DataSource ds = new DataSource(p);

        Connection c = ds.getConnection();
        c.close();
        while(true){
            Thread.sleep(1000);
        }

    }

}
