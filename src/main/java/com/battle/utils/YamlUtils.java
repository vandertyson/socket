package com.battle.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileInputStream;
import java.io.FileWriter;

public class YamlUtils {
    public static <T extends Object> T objectFromYaml(Class<T> type, String inputPath) throws Exception {
        Yaml parser = new Yaml(new Constructor(type));
        try (FileInputStream input = new FileInputStream(inputPath)) {
            return parser.load(input);
        }
    }

    public static void objectToYamlFile(Object obj, String outputFile) throws Exception {
        PropertyUtils propUtils = new PropertyUtils();
        propUtils.setAllowReadOnlyProperties(true);
        propUtils.setSkipMissingProperties(true);
        Representer repr = new Representer();
        repr.setPropertyUtils(propUtils);
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
//        options.setIndent(2);
        options.setSplitLines(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(repr, options);
        yaml.setBeanAccess(BeanAccess.FIELD);
        yaml.dump(obj, new FileWriter(outputFile));
    }

    public static String objectToPrettyYaml(Object obj) {
        PropertyUtils propUtils = new PropertyUtils();
        propUtils.setAllowReadOnlyProperties(true);
        propUtils.setSkipMissingProperties(true);
        Representer repr = new Representer();
        repr.setPropertyUtils(propUtils);
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
//        options.setIndent(4);
        options.setSplitLines(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(repr, options);
        yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml.dump(obj);
    }
}