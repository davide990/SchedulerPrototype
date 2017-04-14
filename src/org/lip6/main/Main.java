package org.lip6.main;

import java.awt.Color;
import java.util.Arrays;
import java.util.Random;

import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.TaskImpl;
import org.lip6.scheduler.TaskFactory;


import java.awt.Color;
import java.util.Random;

public class Main {

	/*public static void main(String[] args) {

		Task t1 = TaskFactory.getTask(0, 0, 0, 0, 1, Arrays.asList(1, 2), (String[] arg) -> {
			System.out.println("hello");
			return null;
		});

		PlanImpl plan = PlanImpl.get(0, 0);
		plan.addTask(t1);

		plan.execute(args);

	}
*/
	
	
	
private static Random random = new Random();
	
	public static void main(String[] args)
    {
        //Color color = new Color(random.nextFloat(), random.nextFloat(), random.nextFloat());
        //String hexColor = Integer.toHexString(color.getRGB());
        
        for (int i=0;i<200;i++)
        {
            Color color = new Color(random.nextFloat(), random.nextFloat(), random.nextFloat()).brighter();
            
            String hexColor = Integer.toHexString(color.getRGB());
            //System.err.println(Integer.toString(i)+",#"+hexColor);
            //System.out.println(Integer.toString(i));
            
            System.out.println(color.getRed()+"\t"+color.getGreen()+"\t"+color.getBlue());
        }
    }
}
