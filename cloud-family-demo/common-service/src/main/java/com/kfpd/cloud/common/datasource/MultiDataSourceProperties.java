package com.kfpd.cloud.common.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.datasource")
public class MultiDataSourceProperties {

    private DataSourcePool cloud = new DataSourcePool();
    private DataSourcePool model = new DataSourcePool();

    public DataSourcePool getCloud() {
        return cloud;
    }

    public void setCloud(DataSourcePool cloud) {
        this.cloud = cloud;
    }

    public DataSourcePool getModel() {
        return model;
    }

    public void setModel(DataSourcePool model) {
        this.model = model;
    }

    public static class DataSourcePool {

        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private String poolName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }
    }
}
