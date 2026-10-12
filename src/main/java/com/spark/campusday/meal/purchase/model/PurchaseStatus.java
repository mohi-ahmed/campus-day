package com.spark.campusday.meal.purchase.model;

/** Processing is retained until stock credits and supplier due are recorded; retry resumes safely. */
public enum PurchaseStatus { PROCESSING, COMPLETED }
