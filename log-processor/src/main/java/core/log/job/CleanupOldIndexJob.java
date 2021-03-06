package core.log.job;

import core.framework.api.scheduler.Job;
import core.framework.api.search.ElasticSearch;
import core.framework.api.search.ElasticSearchIndex;
import core.framework.api.search.SearchException;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author neo
 */
public class CleanupOldIndexJob implements Job {
    private final Logger logger = LoggerFactory.getLogger(CleanupOldIndexJob.class);
    private final Pattern pattern = Pattern.compile("[a-z]+-(\\d{4}-\\d{2}-\\d{2})");
    @Inject
    ElasticSearch elasticSearch;

    @Override
    public void execute() throws Exception {
        LocalDate now = LocalDate.now();

        List<ElasticSearchIndex> indices = elasticSearch.indices();
        for (ElasticSearchIndex index : indices) {
            process(index, now);
        }
    }

    Optional<LocalDate> createdDate(String index) {
        Matcher matcher = pattern.matcher(index);
        if (!matcher.matches()) return Optional.empty();
        String timestamp = matcher.group(1);
        return Optional.of(LocalDate.parse(timestamp));
    }

    private void process(ElasticSearchIndex index, LocalDate now) {
        createdDate(index.index).ifPresent(date -> {
            long days = ChronoUnit.DAYS.between(date, now);
            if (days >= 30) {        // delete log older than 30 days, close index older than 7 days
                deleteIndex(index.index);
            } else if (days >= 7 && index.state == IndexMetaData.State.OPEN) {
                closeIndex(index.index);
            }
        });
    }

    private void deleteIndex(String index) {
        try {
            elasticSearch.deleteIndex(index);
        } catch (SearchException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void closeIndex(String index) {
        try {
            elasticSearch.closeIndex(index);
        } catch (SearchException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
