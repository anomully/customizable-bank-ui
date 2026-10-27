package com.bankquantityorder;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankQuantityOrderPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankQuantityOrderPlugin.class);
		RuneLite.main(args);
	}
}
