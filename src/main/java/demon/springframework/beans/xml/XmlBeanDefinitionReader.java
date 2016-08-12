package demon.springframework.beans.xml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import demon.springframework.BeanReference;
import demon.springframework.beans.AbstractBeanDefinitionReader;
import demon.springframework.beans.BeanDefinition;
import demon.springframework.beans.PropertyValue;
import demon.springframework.beans.factory.config.TestBeanDefinitionHolder;
import demon.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import demon.springframework.beans.factory.support.BeanDefinitionRegistry;
import demon.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import demon.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import demon.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import demon.springframework.beans.factory.xml.NamespaceHandlerResolver;
import demon.springframework.beans.factory.xml.XmlReaderContext;
import demon.springframework.beans.io.Resource;
import demon.springframework.beans.io.ResourceLoader;

/**
 * @author yihua.huang@dianping.com
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {
	
	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;
	
	private NamespaceHandlerResolver namespaceHandlerResolver;
	
	private DefaultBeanDefinitionDocumentReader documentReader =new DefaultBeanDefinitionDocumentReader();
	
	protected final Log logger = LogFactory.getLog(getClass());

	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry,ResourceLoader resourceLoader){
		super(registry,resourceLoader);
	}
	
	public XmlBeanDefinitionReader(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}
	
	public DefaultBeanDefinitionDocumentReader getDocumentReader() {
		return this.documentReader;
	}
	
	//getter setter
	public void setNamespaceHandlerResolver(
			NamespaceHandlerResolver namespaceHandlerResolver) {
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}
	
	//----
	protected XmlReaderContext createReaderContext(Resource resource) {
		if (this.namespaceHandlerResolver == null) {
			this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
		}
		return new XmlReaderContext(resource,this,this.namespaceHandlerResolver);
	}
	
	protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
		return new DefaultNamespaceHandlerResolver(getResourceLoader().getClassLoader());
	}

	@Override
	public void loadBeanDefinitions(String location) throws Exception {
		Resource resource =getResourceLoader().getResource(location);
		InputStream inputStream = getResourceLoader().getResource(location).getInputStream();
		doLoadBeanDefinitions(inputStream,resource);
	}

	protected void doLoadBeanDefinitions(InputStream inputStream,Resource resource) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		Document doc = docBuilder.parse(inputStream);
		// 解析bean
		registerBeanDefinitions(doc,resource);
		inputStream.close();
	}

	public void registerBeanDefinitions(Document doc,Resource resource) {
		Element root = doc.getDocumentElement();
		XmlReaderContext readerContext =createReaderContext(resource);
		getDocumentReader().registerBeanDefinitions(doc, readerContext);
		parseBeanDefinitions(root,resource);
	}

	/**
	 * 源码在这里实现了一种策略模式，来实现对xml解析的可扩展性
	 * delegate分为自定义element和默认element
	 * @param root
	 */
	protected void parseBeanDefinitions(Element root,Resource resource) {
		
		parseBeanDefinitions(root, new BeanDefinitionParserDelegate(getDocumentReader().getReaderContext()));
		NodeList nl = root.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element ele = (Element) node;
				processBeanDefinition(ele);
				parseBeanDefinitions(ele, new BeanDefinitionParserDelegate(getDocumentReader().getReaderContext()));
			}
		}
		
		//方法：预先检测是否为自定义的element，符合走原先逻辑，不符合走策略代理
		//透传，在方法里进行区分
		
	}
	
	/**
	 * 重新增加beanDefinition注册逻辑
	 * @param root
	 * @param delegate
	 */
	protected void parseBeanDefinitions(Element root,BeanDefinitionParserDelegate delegate) {
		//需添加是否为默认元素的判断
		if(delegate.isDefaultNamespace(root)){
			NodeList n1 =root.getChildNodes();
			for (int i = 0; i < n1.getLength(); i++) {
				Node node =n1.item(i);
				if(node instanceof Element){
					Element ele =(Element) node;
					if(delegate.isDefaultNamespace(ele)){
						parseDefaultElement(ele,delegate);
					}
				}
			}
		}
		else{
			delegate.parseCustomElement(root);
		}
	}
	

	private void parseDefaultElement(Element ele,BeanDefinitionParserDelegate delegate) {
		if(delegate.nodeNameEquals(ele, BEAN_ELEMENT)){
			processBeanDefinition(ele,delegate);
		}
	}

	
	protected void processBeanDefinition(Element ele,BeanDefinitionParserDelegate delegate) {
		TestBeanDefinitionHolder bdholder =delegate.parseBeanDefinitionElement(ele);
		if(bdholder !=null){
			//需要注册到beanFactory所实现的注册接口中去
			try {
				BeanDefinitionReaderUtils.registerBeanDefinition(bdholder, getBeanFactory());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

	protected void processBeanDefinition(Element ele) {
		String name = ele.getAttribute("id");
		String className = ele.getAttribute("class");
		//判断下 如果name 和 className为空的话返回
		if(name.equals("") || className.equals("")){
			return;
		}
		BeanDefinition beanDefinition = new BeanDefinition();
		processProperty(ele, beanDefinition);
		beanDefinition.setBeanClassName(className);
		//需要注册在beanfactory中去
		getRegistry().put(name, beanDefinition);
	}

	//需要一套更完善的xml node文件的解析
	private void processProperty(Element ele, BeanDefinition beanDefinition) {
		NodeList propertyNode = ele.getElementsByTagName("property");
		for (int i = 0; i < propertyNode.getLength(); i++) {
			Node node = propertyNode.item(i);
			if (node instanceof Element) {
				Element propertyEle = (Element) node;
				//获得了对应的set方法的名字
				String name = propertyEle.getAttribute("name");
				//解析策略需要判断是否存在value和子节点
				String value = propertyEle.getAttribute("value");
				//解析是否为ref属性
				String ref =propertyEle.getAttribute("ref");
				//判断该节点是否还有子节点
				NodeList props =propertyEle.getElementsByTagName("props");
				if (value != null && value.length() > 0) {
					beanDefinition.getPropertyValues().addPropertyValue(new PropertyValue(name, value));
				} 
				else if(ref !=null && ref.length() >0){//value的值为空,则需要判断是否是ref
					//封装了一个统一的引用对象,来处理propertyValue
					BeanReference beanReference = new BeanReference(ref);
					beanDefinition.getPropertyValues().addPropertyValue(new PropertyValue(name, beanReference));
				}
				else if(props !=null){//如果还存在子节点,就需处理property的子节点,最好还是根据name来判断来得可靠一些
					processProps(props,beanDefinition,name);
				}
				else{
					throw new IllegalArgumentException("Configuration problem: <property> element for property '"
							+ name + "' must specify a ref or value");
				}
			}
		}
	}
	
	//处理props数组
	private void processProps(NodeList props,BeanDefinition beanDefinition,String name){
		for (int i = 0; i < props.getLength(); i++) {
			Node node = props.item(i);
			//针对每一个节点,又需要for循环进行处理字节点
			if (node instanceof Element) {
				Element propsEle = (Element) node;
				NodeList prop =propsEle.getElementsByTagName("prop");
				Map<String, Object> urlMap =new HashMap<String, Object>();
				for (int j = 0; j < prop.getLength(); j++) {
					Node pNode = prop.item(j);
					//针对每一个节点,又需要for循环进行处理字节点
					if (pNode instanceof Element) {
						Element propEle = (Element) pNode;
						//获得了对应的set方法的名字,并且明确了是对应的键值对
						String key = propEle.getAttribute("key");
						String value =propEle.getFirstChild().getTextContent();
						if (value != null && value.length() > 0) {
							//封装一个object对象 ? 统一的键值对数据格式来得靠谱
							urlMap.put(key, value);
						}else {
							throw new IllegalArgumentException("Configuration problem: <prop> element for prop '"
									+ key + "' must specify a TextContent value");
						}
					}
				}
				beanDefinition.getPropertyValues().addPropertyValue(new PropertyValue(name, urlMap));
			}
		}
	}
}
