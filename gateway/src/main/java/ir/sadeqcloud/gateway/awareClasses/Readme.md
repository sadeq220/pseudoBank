Typically spring’s DI container does following things to create a bean, you request for:

    Create the bean instance either by a constructor or by a factory method
    Set the values and bean references to the bean properties
    Call the setter methods defined in the all the aware interfaces
    Pass the bean instance to the postProcessBeforeInitialization() method of each bean post processor
    Call the initialization callback methods
    Pass the bean instance to the postProcessAfterInitialization() method of each bean post processor
    The bean is ready to be used
    When the container is shut down, call the destruction callback methods
### BeanPostProcessor Vs BeanFactoryPostProcessor
#### from stackoverflow 

    A bean implementing BeanFactoryPostProcessor is called when all bean definitions will have been loaded, but no beans will have been instantiated yet. This allows for overriding or adding properties even to eager-initializing beans. This will let you have access to all the beans that you have defined in XML or that are annotated (scanned via component-scan).
    A bean implementing BeanPostProcessor operate on bean (or object) instances which means that when the Spring IoC container instantiates a bean instance then BeanPostProcessor interfaces do their work.
    BeanFactoryPostProcessor implementations are "called" during startup of the Spring context after all bean definitions will have been loaded while BeanPostProcessor are "called" when the Spring IoC container instantiates a bean (i.e. during the startup for all the singleton and on demand for the proptotypes one)
