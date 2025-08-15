package org.btik.espidf.util;


import org.apache.commons.collections.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TreeUtils {

    public static <T extends TreeBean<T>> void treeEach(T root, Consumer<T> consumer) {
        LinkedList<T> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            T nodeModel = queue.removeFirst();
            consumer.accept(nodeModel);
            List<T> children = nodeModel.getChildren();
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            queue.addAll(children);
        }
    }

    public static <T extends TreeBean<T>> void treeEachWithBreak(T root, Function<T, Boolean> consumer) {
        LinkedList<T> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            T nodeModel = queue.removeFirst();
            Boolean next = consumer.apply(nodeModel);
            if (!next) {
                break;
            }
            List<T> children = nodeModel.getChildren();
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            queue.addAll(children);
        }
    }

    public static <T extends TreeBean<T>> void eachWithParent(T root, BiConsumer<T, T> consumer) {
        LinkedList<T> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            T nodeModel = queue.removeFirst();
            List<T> children = nodeModel.getChildren();
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            for (T child : children) {
                consumer.accept(nodeModel, child);
                queue.add(child);
            }
        }
    }
}
