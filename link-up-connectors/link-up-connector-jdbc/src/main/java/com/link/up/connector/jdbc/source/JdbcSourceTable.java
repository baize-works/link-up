package com.link.up.connector.jdbc.source;


import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class JdbcSourceTable implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TablePath tablePath;
    private final String query;
    private final String partitionColumn;
    private final Integer partitionNumber;
    private final String partitionStart;
    private final String partitionEnd;
    private final Boolean useSelectCount;
    private final Boolean skipAnalyze;
    private final CatalogTable catalogTable;
}

