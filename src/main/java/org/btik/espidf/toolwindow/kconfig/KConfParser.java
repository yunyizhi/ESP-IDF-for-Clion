package org.btik.espidf.toolwindow.kconfig;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.btik.espidf.toolwindow.common.NodeModel;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author lustre
 * @since 2025/6/15 21:31
 */
public class KConfParser {
    private static final Logger LOG = Logger.getInstance(KConfParser.class);
    public static List<ConfModel> parseKconfig(Path path) {
        Gson gson = new Gson();
        try {
            JsonElement jsonElement = gson.fromJson(new JsonReader(new FileReader(path.toFile())), JsonElement.class);
            List<ConfModel> root = new ArrayList<>();
            if (jsonElement.isJsonArray()) {
                JsonArray asJsonArray = jsonElement.getAsJsonArray();
                for (JsonElement element : asJsonArray) {
                    JsonObject asJsonObject = element.getAsJsonObject();
                    ConfModel confModel = gson.fromJson(asJsonObject, ConfModel.class);
                    root.add(confModel);
                }
            }
            return root;

        } catch (FileNotFoundException e) {
            LOG.error("File does not exist");
        }
        return List.of();
    }

    public static Map<String,Object> parseSdkConfig(Path path) {
        Gson gson = new Gson();
        try {
            JsonElement jsonElement = gson.fromJson(new JsonReader(new FileReader(path.toFile())), JsonElement.class);
            Map<String,Object> result = new HashMap<>();
            if (jsonElement.isJsonObject()) {
                jsonElement.getAsJsonObject().entrySet().forEach(entry -> {
                    JsonElement value = entry.getValue();
                    String key = entry.getKey();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive asJsonPrimitive = value.getAsJsonPrimitive();
                        if (asJsonPrimitive.isBoolean()) {
                            result.put(key, asJsonPrimitive.getAsBoolean());
                        }else if (asJsonPrimitive.isString()) {
                            result.put(key, asJsonPrimitive.getAsString());
                        }else if (asJsonPrimitive.isNumber()) {
                            result.put(key, asJsonPrimitive.getAsLong());
                        }
                    }
                });
            }
            return result;

        } catch (FileNotFoundException e) {
            LOG.error("File does not exist");
        }
        return Map.of();
    }


    public static DefaultMutableTreeNode buildTree(ConfModel confModel){
        if (!confModel.isVisible() || confModel.isLeaf()){
            return null;
        }
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(confModel);
        LinkedList<NodeModel<ConfModel>> queue = new LinkedList<>();
        NodeModel<ConfModel> rootNodeModel = new NodeModel<>(treeNode, confModel);
        queue.add(rootNodeModel);
        while (!queue.isEmpty()) {
            NodeModel<ConfModel> nodeModel = queue.removeFirst();
            List<ConfModel> children = nodeModel.getModel().getChildren();
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            for (ConfModel child : children) {
                if (!child.isVisible() || child.isLeaf()) {
                    continue;
                }
                DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(child);
                queue.add(new NodeModel<>(childTreeNode, child));
                nodeModel.getNode().add(childTreeNode);
            }
        }
        return treeNode;
    }

    public static void treeEach(ConfModel confModel, Consumer<ConfModel> consumer){
        LinkedList<ConfModel> queue = new LinkedList<>();
        queue.add(confModel);
        while (!queue.isEmpty()) {
            ConfModel nodeModel = queue.removeFirst();
            consumer.accept(nodeModel);
            List<ConfModel> children = nodeModel.getChildren();
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            queue.addAll(children);
        }
    }

    public static void eachWithParent(ConfModel confModel, BiConsumer<ConfModel,ConfModel> consumer){
        LinkedList<ConfModel> queue = new LinkedList<>();
        queue.add(confModel);
        while (!queue.isEmpty()) {
            ConfModel nodeModel = queue.removeFirst();
            List<ConfModel> children = nodeModel.getChildren();
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            for (ConfModel child : children) {
                consumer.accept(nodeModel, child);
            }
        }
    }
}
