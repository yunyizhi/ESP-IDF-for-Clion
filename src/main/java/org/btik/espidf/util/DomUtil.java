package org.btik.espidf.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author lustre
 * @since 2022/10/21 1:02
 */
public class DomUtil {
    /**
     * 将整个xml解析为dom对象
     *
     * @param path xml路径
     * @return dom对象
     */
    public static Document parse(String path) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(path);
    }

    /**
     * 将整个xml解析为dom对象
     *
     * @param xmlStream xml输入流
     * @return dom对象
     */
    public static Document parse(InputStream xmlStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(xmlStream);
    }

    /**
     * 将整个xml解析为dom对象
     *
     * @param xmlFile xml文件
     * @return dom对象
     */
    public static Document parse(File xmlFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(xmlFile);
    }

    /**
     * 遍历所有子元素
     *
     * @param consumer 回调函数
     */
    public static void eachChildrenElement(Element element, Consumer<Element> consumer) {
        NodeList childNodes = element.getChildNodes();
        int length = childNodes.getLength();
        for (int i = 0; i < length; i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                consumer.accept((Element) item);
            }
        }
    }

    /**
     * 通过标签名遍历元素
     *
     * @param consumer 回调函数
     */
    public static void eachByTagName(Element element, String tagName, Consumer<Element> consumer) {
        NodeList childNodes = element.getElementsByTagName(tagName);
        int length = childNodes.getLength();
        for (int i = 0; i < length; i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                consumer.accept((Element) item);
            }
        }
    }

    /**
     * 获取子元素
     *
     * @param element 父元素
     * @param tagName 标签名
     */
    public static List<Element> getElementByName(Element element, String tagName) {
        List<Element> result = new ArrayList<>();
        eachByTagName(element, tagName, (item) -> result.add(element));
        return result;
    }

    /**
     * 获取子元素
     *
     * @param element 父元素
     * @param tagName 标签名
     */
    public static Element getFirstElementByName(Element element, String tagName) {
        NodeList childNodes = element.getElementsByTagName(tagName);
        int length = childNodes.getLength();
        for (int i = 0; i < length; i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                return (Element) item;
            }
        }
        return null;
    }

}
