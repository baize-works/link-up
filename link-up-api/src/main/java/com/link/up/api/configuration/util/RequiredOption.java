package com.link.up.api.configuration.util;

import com.link.up.api.configuration.Option;

import java.util.*;

import static com.link.up.api.configuration.util.OptionUtil.getOptionKeys;

/**
 * 必填配置规则。
 */
public interface RequiredOption {

    List<Option<?>> getOptions();

    /**
     * 必须配置的选项。
     */
    final class AbsolutelyRequiredOptions
            implements RequiredOption {

        private final List<Option<?>> options;

        private AbsolutelyRequiredOptions(
                List<Option<?>> options) {
            this.options = Collections.unmodifiableList(
                    new ArrayList<>(options)
            );
        }

        public static AbsolutelyRequiredOptions of(
                Option<?>... options) {

            return new AbsolutelyRequiredOptions(
                    Arrays.asList(options));
        }

        public List<Option<?>> getRequiredOption() {
            return options;
        }

        @Override
        public List<Option<?>> getOptions() {
            return options;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AbsolutelyRequiredOptions)) {
                return false;
            }

            AbsolutelyRequiredOptions that =
                    (AbsolutelyRequiredOptions) obj;

            return Objects.equals(options, that.options);
        }

        @Override
        public int hashCode() {
            return options.hashCode();
        }

        @Override
        public String toString() {
            return "Required options: "
                    + getOptionKeys(options);
        }
    }

    /**
     * 互斥选项，只能配置一个。
     */
    final class ExclusiveRequiredOptions
            implements RequiredOption {

        private final List<Option<?>> options;

        private ExclusiveRequiredOptions(
                List<Option<?>> options) {
            this.options = Collections.unmodifiableList(
                    new ArrayList<>(options)
            );
        }

        public static ExclusiveRequiredOptions of(
                Option<?>... options) {

            return new ExclusiveRequiredOptions(
                    Arrays.asList(options));
        }

        public List<Option<?>> getExclusiveOptions() {
            return options;
        }

        @Override
        public List<Option<?>> getOptions() {
            return options;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ExclusiveRequiredOptions)) {
                return false;
            }

            ExclusiveRequiredOptions that =
                    (ExclusiveRequiredOptions) obj;

            return Objects.equals(options, that.options);
        }

        @Override
        public int hashCode() {
            return options.hashCode();
        }

        @Override
        public String toString() {
            return "Exclusive options: "
                    + getOptionKeys(options);
        }
    }

    /**
     * 绑定选项，必须同时存在或同时不存在。
     */
    final class BundledRequiredOptions
            implements RequiredOption {

        private final List<Option<?>> options;

        private BundledRequiredOptions(
                List<Option<?>> options) {
            this.options = Collections.unmodifiableList(
                    new ArrayList<>(options)
            );
        }

        public static BundledRequiredOptions of(
                Option<?>... options) {

            return new BundledRequiredOptions(
                    Arrays.asList(options));
        }

        public static BundledRequiredOptions of(
                List<Option<?>> options) {

            return new BundledRequiredOptions(options);
        }

        public List<Option<?>> getRequiredOption() {
            return options;
        }

        @Override
        public List<Option<?>> getOptions() {
            return options;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BundledRequiredOptions)) {
                return false;
            }

            BundledRequiredOptions that =
                    (BundledRequiredOptions) obj;

            return Objects.equals(options, that.options);
        }

        @Override
        public int hashCode() {
            return options.hashCode();
        }

        @Override
        public String toString() {
            return "Bundled options: "
                    + getOptionKeys(options);
        }
    }

    /**
     * 条件成立时必填的选项。
     */
    final class ConditionalRequiredOptions
            implements RequiredOption {

        private final Condition<?> condition;
        private final List<Option<?>> options;

        private ConditionalRequiredOptions(
                Condition<?> condition,
                List<Option<?>> options) {

            this.condition = Objects.requireNonNull(
                    condition,
                    "condition");

            this.options = Collections.unmodifiableList(
                    new ArrayList<>(options)
            );
        }

        public static ConditionalRequiredOptions of(
                Condition<?> condition,
                List<Option<?>> options) {

            return new ConditionalRequiredOptions(
                    condition,
                    options);
        }

        public Condition<?> getCondition() {
            return condition;
        }

        public List<Option<?>> getRequiredOption() {
            return options;
        }

        @Override
        public List<Option<?>> getOptions() {
            return options;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ConditionalRequiredOptions)) {
                return false;
            }

            ConditionalRequiredOptions that =
                    (ConditionalRequiredOptions) obj;

            return Objects.equals(condition, that.condition)
                    && Objects.equals(options, that.options);
        }

        @Override
        public int hashCode() {
            return Objects.hash(condition, options);
        }

        @Override
        public String toString() {
            return String.format(
                    "Condition: %s, required options: %s",
                    condition,
                    getOptionKeys(options));
        }
    }
}