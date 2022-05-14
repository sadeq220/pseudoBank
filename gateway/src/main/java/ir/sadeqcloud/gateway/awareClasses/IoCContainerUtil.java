package ir.sadeqcloud.gateway.awareClasses;

import ir.sadeqcloud.gateway.model.TransferResponse;
import ir.sadeqcloud.gateway.sharedResource.IntermediaryObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class IoCContainerUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static BeanDefinitionRegistry beanDefinitionRegistry;
    /**
     * After All beans has been created this method will be called
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext=applicationContext;
        AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        IoCContainerUtil.beanDefinitionRegistry = (BeanDefinitionRegistry) autowireCapableBeanFactory;
    }
    public static <T> T getBean(Class<T> tClass){
        return applicationContext.getBean(tClass);
    }
    public static <T> T getBean(Class<T> tClass,String beanName){
        return applicationContext.getBean(beanName,tClass);
    }

    /**
     * dynamically add the bean to spring-context
     * To add bean to Ioc container at runtime we must create  instance of BeanDefinition and,
     * register it by BeanDefinitionRegistry#registerBeanDefinition()
     *
     * A BeanDefinition describes a bean instance, which has property values, constructor argument values, and further information supplied by concrete implementations.
     */
    public static void registerBean(String beanName,String accountNo,String correlationId){

        GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
        genericBeanDefinition.setBeanClass(IntermediaryObject.class);
        genericBeanDefinition.getPropertyValues().addPropertyValue("accountNo",accountNo);
        genericBeanDefinition.getPropertyValues().addPropertyValue("correlationId",correlationId);

        beanDefinitionRegistry.registerBeanDefinition(beanName, genericBeanDefinition);
    }
    public static void unregisterBean(String beanName){
        beanDefinitionRegistry.removeBeanDefinition(beanName);
    }
}
