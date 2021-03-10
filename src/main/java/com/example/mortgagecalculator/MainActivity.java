package com.example.mortgagecalculator;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {

    private EditText et_price, et_loan, et_business, et_accumulation;
    private TextView tv_loan;
    private RadioGroup rg_payment;
    private CheckBox ck_business, ck_accumulation;
    private TextView tv_payment;

    private int mYear;
    private double mBusinessRatio;
    private double mAccumulationRatio;
    private boolean isInterest;
    private boolean hasBusiness, hasAccumulation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_price = findViewById(R.id.et_price);
        et_loan = findViewById(R.id.et_loan);
        tv_loan = findViewById(R.id.tv_loan);
        rg_payment = findViewById(R.id.rg_payment);
        rg_payment.setOnCheckedChangeListener(this);
        ck_business = findViewById(R.id.ck_business);
        ck_business.setOnCheckedChangeListener(this);
        et_business = findViewById(R.id.et_business);
        ck_accumulation = findViewById(R.id.ck_accumulation);
        ck_accumulation.setOnCheckedChangeListener(this);
        et_accumulation = findViewById(R.id.et_accumulation);
        tv_payment = findViewById(R.id.tv_payment);
        findViewById(R.id.btn_loan).setOnClickListener(this);
        findViewById(R.id.btn_calculate).setOnClickListener(this);
        initYearSpinner();
        initRatioSpinner();
    }

    // 初始化贷款年限下拉框
    private void initYearSpinner() {
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<String>(this, R.layout.item_select, DataUtil.yearDescArray);
        Spinner sp_year = findViewById(R.id.sp_year);
        sp_year.setPrompt("请选择贷款年限");
        sp_year.setAdapter(yearAdapter);
        sp_year.setSelection(0);
        sp_year.setOnItemSelectedListener(new YearSelectedListener());
    }

    // 定义一个贷款年限的选择监听器
    class YearSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mYear = DataUtil.yearArray[position];
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    // 初始化基准利率下拉框
    private void initRatioSpinner() {
        ArrayAdapter<String> ratioAdapter = new ArrayAdapter<String>(this, R.layout.item_select, DataUtil.radioDescArray);
        Spinner sp_ratio = findViewById(R.id.sp_ratio);
        sp_ratio.setPrompt("请选择基准利率");
        sp_ratio.setAdapter(ratioAdapter);
        sp_ratio.setSelection(0);
        sp_ratio.setOnItemSelectedListener(new RadioSelectedListener());
    }

    // 定义一个基准利率的选择监听器
    class RadioSelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mBusinessRatio = DataUtil.businessArray[position];
            mAccumulationRatio = DataUtil.accumulationArray[position];
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_loan:
                if (TextUtils.isEmpty(et_price.getText().toString())) {
                    Toast.makeText(this, "购房总价不能为空", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (TextUtils.isEmpty(et_loan.getText().toString())) {
                    Toast.makeText(this, "按揭部分不能为空", Toast.LENGTH_SHORT).show();
                    break;
                }
                showLoad(); // 显示计算好的贷款总额
                break;
            case R.id.btn_calculate:
                if (hasBusiness && TextUtils.isEmpty(et_business.getText().toString())) {
                    Toast.makeText(this, "商业贷款总额不能为空", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (hasAccumulation && TextUtils.isEmpty(et_accumulation.getText().toString())) {
                    Toast.makeText(this, "公积金贷款总额不能为空", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!hasBusiness && !hasAccumulation) {
                    Toast.makeText(this, "请选择商业贷款或公积金贷款", Toast.LENGTH_SHORT).show();
                    break;
                }
                showRepayment();
                break;
        }
    }

    // 根据购房总价和按揭比例，计算贷款总额
    private void showLoad() {
        double total = Double.parseDouble(et_price.getText().toString());
        double rate = Double.parseDouble(et_loan.getText().toString()) / 100;
        String desc = String.format("您的贷款总额为%s万元", formatDecimal(total * rate, 2));
        tv_loan.setText(desc);
    }

    // 根据贷款的相关条件，计算还款总额、利息总额，以及月供
    private void showRepayment() {
        Repayment businessResult = new Repayment();
        Repayment accumulationResult = new Repayment();
        if (hasBusiness) {  //申请了商业贷款
            double bussinessLoan = Double.parseDouble(et_business.getText().toString());
            double bussinessTime = mYear * 12;
            double bussinessRate = mBusinessRatio / 100;

            // 计算商业贷款部分的还款明细
            businessResult = calMortgage(bussinessLoan, bussinessTime, bussinessRate, isInterest);
        }
        if (hasAccumulation) {  //申请了公积金贷款
            double accumulationLoan = Double.parseDouble(et_accumulation.getText().toString());
            double accumulationTime = mYear * 12;
            double accumulationRate = mAccumulationRatio / 100;

            // 计算公积金贷款部分的还款明细
            accumulationResult = calMortgage(accumulationLoan, accumulationTime, accumulationRate, isInterest);
        }
        String desc = String.format("您的贷款总额为%s万元", formatDecimal((businessResult.mTotal + accumulationResult.mTotal) / 10000, 2));
        desc = String.format("%s\n  还款总额为%s万元", desc, formatDecimal((businessResult.mTotal + businessResult.mTotalInterest
                + accumulationResult.mTotal + accumulationResult.mTotalInterest) / 10000, 2));
        desc = String.format("%s\n其中利息总额为%s万元", desc, formatDecimal((businessResult.mTotalInterest + accumulationResult.mTotalInterest) / 10000, 2));
        desc = String.format("%s\n  还款总时间为%d月", desc, mYear * 12);
        if (isInterest) { // 如果是等额本息方式
            desc = String.format("%s\n每月还款金额为%s元", desc, formatDecimal(businessResult.mMonthRepayment + accumulationResult.mMonthRepayment, 2));
        } else { // 如果是等额本金方式
            desc = String.format("%s\n首月还款金额为%s元，其后每月递减%s元", desc, formatDecimal(businessResult.mMonthRepayment + accumulationResult.mMonthRepayment, 2),
                    formatDecimal(businessResult.mMonthMinus + accumulationResult.mMonthMinus, 2));
        }
        tv_payment.setText(desc);
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_interest:
                isInterest = true;
                break;
            case R.id.rb_principal:
                isInterest = false;
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.ck_business:
                hasBusiness = isChecked;
            case R.id.ck_accumulation:
                hasAccumulation = isChecked;
        }
    }

    // 精确到小数点后第几位
    private String formatDecimal(double value, int digit) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(digit, RoundingMode.HALF_UP);
        return bd.toString();
    }

    // 根据贷款金额、还款年限、基准利率，计算还款信息
    public Repayment calMortgage(double loadAmount, double loanMonths, double rate, boolean bInterest) {

        // 等额本息
        double amountMonth = (loadAmount * rate / 12 * Math.pow((1 + rate / 12), loanMonths)) / (Math.pow((1 + rate / 12), loanMonths) - 1);
        double amount = amountMonth * loanMonths;
        double rateAmount = amount - loadAmount;

        // 等额本金 每月还款本金、每月还款利息、每月还款、每月利息差额
        double principalMonth = loadAmount / loanMonths;
        double interestMonth = loadAmount * (rate / 12);
        double repaymentMonth = principalMonth + interestMonth;
        double diff = principalMonth * (rate / 12);
        double realMonthPrincipal = principalMonth + diff;
        double av = (realMonthPrincipal + repaymentMonth) / 2;
        double total = av * loanMonths;
        double totalInterest = total - loadAmount;

        Repayment result = new Repayment();
        if (bInterest) {
            result.mMonthRepayment = amountMonth;
            result.mTotalInterest = rateAmount;
        } else {
            result.mMonthRepayment = repaymentMonth;
            result.mMonthMinus = diff;
            result.mTotalInterest = totalInterest;
        }
        return result;
    }
}
