package edu.dartmouth.geisel.isidro.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class TestUtils
{
    public static final String WORKSHEET_NAME = "isidro";

    /**
     * Private constructor of utilities class.
     */
    private TestUtils()
    {
    }

    public static void generateExcelFile(final XSSFWorkbook workbook,
            final String absFilePath) throws FileNotFoundException, IOException
    {

        try (FileOutputStream fos = new FileOutputStream(new File(absFilePath)))
        {
            workbook.write(fos);
        }
    }

    public static void initPrivateConstructor(final Class<?> clazz)
    {
        try
        {
            final Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            constructor.setAccessible(false);
        } catch (final SecurityException | InstantiationException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException e)
        {
            // Intentionally left blank
        }
    }

    public static void invokePrivateStaticMethod(final Class<?> clazz,
            final String methodName, final Class<?>[] params,
            final Object[] args)
    {

        try
        {
            final Method method = clazz.getDeclaredMethod(methodName, params);
            method.setAccessible(true);
            method.invoke(null, args);
            method.setAccessible(false);
        } catch (final SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e)
        {
            // Intentionally left blank
        }
    }

    public static void walkCsvsPerformTestAction(
            final Consumer<File> testConsumer)
    {

        final File parent = new File(
                TestUtils.class.getClass().getResource("/").getFile());
        File[] children = null;
        if (parent.isDirectory())
        {
            children = parent.listFiles((FileFilter) file -> file.getName()
                    .toLowerCase().endsWith(".csv"));
        }

        for (final File file : children)
        {
            testConsumer.accept(file);
        }
    }
}
