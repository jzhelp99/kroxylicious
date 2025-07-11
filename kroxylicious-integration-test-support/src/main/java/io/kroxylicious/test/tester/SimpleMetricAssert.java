/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.test.tester;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.FactoryBasedNavigableListAssert;
import org.assertj.core.api.MapAssert;

import static org.assertj.core.util.Lists.newArrayList;

public class SimpleMetricAssert extends AbstractAssert<SimpleMetricAssert, SimpleMetric> {

    public static SimpleMetricAssert assertThat(SimpleMetric actual) {
        return new SimpleMetricAssert(actual);
    }

    public static SimpleMetricListAssert assertThat(List<SimpleMetric> actual) {
        return new SimpleMetricListAssert(actual);
    }

    private SimpleMetricAssert(SimpleMetric simpleMetric) {
        super(simpleMetric, SimpleMetricAssert.class);
        describedAs(simpleMetric == null ? "null metric" : "metric");
    }

    @SuppressWarnings("java:S1452")
    public AbstractDoubleAssert<?> value() {
        isNotNull();
        return Assertions.assertThat(actual.value())
                .describedAs("metric value");
    }

    @SuppressWarnings("java:S1452")
    public AbstractStringAssert<?> name() {
        isNotNull();
        return Assertions.assertThat(actual.name())
                .describedAs("metric name");
    }

    public MapAssert<String, String> labels() {
        isNotNull();
        return Assertions.assertThat(actual.labels())
                .describedAs("metric labels");
    }

    @SuppressWarnings("java:S110") // Ignoring "This class has 6 parents which is greater than 5 authorized" as this is the Assert-J public API.
    public static class SimpleMetricListAssert extends FactoryBasedNavigableListAssert<SimpleMetricListAssert, List<SimpleMetric>, SimpleMetric, SimpleMetricAssert> {
        private SimpleMetricListAssert(List<SimpleMetric> simpleMetrics) {
            super(simpleMetrics, SimpleMetricListAssert.class, SimpleMetricAssert::new);
            describedAs(simpleMetrics == null ? "empty metric list" : "metrics");

        }

        public SimpleMetricListAssert filterByName(String name) {
            isNotNull();
            return filteredOn(metric -> metric.name().equals(name));
        }

        public SimpleMetricListAssert filterByTag(String key) {
            isNotNull();
            return filteredOn(metric -> metric.labels() != null
                    && metric.labels().containsKey(key));
        }

        public SimpleMetricListAssert filterByTag(String key, String value) {
            isNotNull();
            return filterByTag(key)
                    .filteredOn(metric -> Objects.equals(metric.labels().get(key), value));
        }

        public SimpleMetricAssert withUniqueMetric(String name, Map<String, String> tags) {
            isNotNull();
            Assertions.assertThat(tags).isNotNull();
            return describedAs("no metrics match by name [%s]", name)
                    .filterByName(name)
                    .hasSizeGreaterThan(0)
                    .describedAs("one or more metrics match by name, but none have all of the expected tag names [%s]", String.join(",", tags.keySet()))
                    .filteredOn(sm -> sm.labels().keySet().containsAll(tags.keySet()))
                    .hasSizeGreaterThan(0)
                    .describedAs("one or more metrics match by name, but none have all of the expected tag name/value pairs [%s]",
                            tags.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(
                                    Collectors.joining(",")))
                    .filteredOn(sm -> {
                        var copyTags = new HashMap<>(tags);
                        var copyMetricLabels = new HashMap<>(sm.labels());
                        copyMetricLabels.keySet().retainAll(tags.keySet());
                        return Objects.equals(copyMetricLabels, copyTags);
                    })
                    .singleElement();
        }

        public SimpleMetricListAssert hasNoMetricMatching(String name, Map<String, String> tags) {
            isNotNull();
            Assertions.assertThat(tags).isNotNull();

            var smla = describedAs("found unexpected metric")
                    .filterByName(name);
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                smla = smla.filterByTag(entry.getKey(), entry.getValue());
            }
            smla.isEmpty();
            return this;
        }

        @Override
        protected SimpleMetricListAssert newAbstractIterableAssert(Iterable<? extends SimpleMetric> iterable) {
            return new SimpleMetricListAssert(newArrayList(iterable));
        }
    }
}
