package com.landlord.android.feature.reports;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ReportApiService {

    @GET("api/reports/income-statement")
    Call<ResponseBody> incomeStatement(@Query("startDate") String startDate, @Query("endDate") String endDate,
                                        @Query("propertyId") Long propertyId);

    @Streaming
    @GET("api/reports/income-statement.pdf")
    Call<ResponseBody> incomeStatementPdf(@Query("startDate") String startDate, @Query("endDate") String endDate,
                                           @Query("propertyId") Long propertyId);

    @Streaming
    @GET("api/reports/income-statement.xlsx")
    Call<ResponseBody> incomeStatementXlsx(@Query("startDate") String startDate, @Query("endDate") String endDate,
                                            @Query("propertyId") Long propertyId);

    @GET("api/reports/expense-report")
    Call<ResponseBody> expenseReport(@Query("startDate") String startDate, @Query("endDate") String endDate,
                                      @Query("propertyId") Long propertyId, @Query("category") String category);

    @Streaming
    @GET("api/reports/expense-report.pdf")
    Call<ResponseBody> expenseReportPdf(@Query("startDate") String startDate, @Query("endDate") String endDate,
                                         @Query("propertyId") Long propertyId, @Query("category") String category);

    @Streaming
    @GET("api/reports/expense-report.xlsx")
    Call<ResponseBody> expenseReportXlsx(@Query("startDate") String startDate, @Query("endDate") String endDate,
                                          @Query("propertyId") Long propertyId, @Query("category") String category);

    @GET("api/reports/occupancy-report")
    Call<ResponseBody> occupancyReport(@Query("propertyId") Long propertyId);

    @Streaming
    @GET("api/reports/occupancy-report.pdf")
    Call<ResponseBody> occupancyReportPdf(@Query("propertyId") Long propertyId);

    @Streaming
    @GET("api/reports/occupancy-report.xlsx")
    Call<ResponseBody> occupancyReportXlsx(@Query("propertyId") Long propertyId);

    @GET("api/reports/tenant-ledger")
    Call<ResponseBody> tenantLedger(@Query("tenantId") Long tenantId, @Query("startDate") String startDate,
                                     @Query("endDate") String endDate);

    @Streaming
    @GET("api/reports/tenant-ledger.pdf")
    Call<ResponseBody> tenantLedgerPdf(@Query("tenantId") Long tenantId, @Query("startDate") String startDate,
                                        @Query("endDate") String endDate);

    @Streaming
    @GET("api/reports/tenant-ledger.xlsx")
    Call<ResponseBody> tenantLedgerXlsx(@Query("tenantId") Long tenantId, @Query("startDate") String startDate,
                                         @Query("endDate") String endDate);

    @Streaming
    @GET("api/reports/full-report.pdf")
    Call<ResponseBody> fullReportPdf(@Query("startDate") String startDate, @Query("endDate") String endDate,
                                      @Query("propertyId") Long propertyId);

    @Streaming
    @GET("api/reports/full-report.xlsx")
    Call<ResponseBody> fullReportXlsx(@Query("startDate") String startDate, @Query("endDate") String endDate,
                                       @Query("propertyId") Long propertyId);
}
