package com.example.homework314chcortes;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends Activity {
	
	
	WeatherGatherer weatherGatherer;
	
	//Handler for message sent to UI thread when list data is collected from online
	// Handler for updating the UI
	private Handler WeatherLoadCompleteHandler = new Handler() {
	       @Override
	       public void handleMessage(Message msg) {
	           super.handleMessage(msg);
	           
	           String todayUpdate = msg.getData().getString(WeatherGatherer.TODAYWEATHERLOADCOMPLETE);
	           if (todayUpdate != null){
	           	setTodaysWeather();
	           }
	           
	           String forecastUpdate = msg.getData().getString(WeatherGatherer.FORECASTWEATHERLOADCOMPLETE);
	           if (forecastUpdate != null){
	           	setForecastWeather();
	           }
	           
	           String updateError = msg.getData().getString(WeatherGatherer.LOADERROR);
	           if (updateError != null){
	           	setConnectivityErrorMessage();
	           }
	       }               
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		gatherWeatherData();
		
	}

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/* Sets the Today's Weather UI */
	private void setTodaysWeather() {
		WeatherDayInfo TodaysWeatherInfo = weatherGatherer.todaysWeather;
		
		//Set the date
		TextView dateTextView = (TextView) this.findViewById(R.id.today_date);
		dateTextView.setText(TodaysWeatherInfo.date);
		
		//Set the temperature
		TextView temperatureTextView = (TextView) this.findViewById(R.id.today_temperature);
		temperatureTextView.setText(TodaysWeatherInfo.temperature);
		
		//Set the Conditions
		TextView conditionsTextView = (TextView) this.findViewById(R.id.today_conditions);
		conditionsTextView.setText(TodaysWeatherInfo.conditions);
		
		//Set the Image
		WebView imageWebView = (WebView) this.findViewById(R.id.today_image);
		imageWebView.getSettings().setJavaScriptEnabled(false);
		imageWebView.loadUrl(TodaysWeatherInfo.imageUrl);
		
		//Hide the progress bar, set the weather ui fields to visible
		hideTodayWeatherProgressBar();
		showTodayWeatherViews();
		
		return;
	}
	
	/* Set the forecast Weather UI*/
	private void setForecastWeather(){
		//Day 1
		setForecastDayWeather(1, R.id.day_one_date, R.id.day_one_conditions, R.id.day_one_image, R.id.day_one_high_temperature, R.id.day_one_low_temperature);
		
		//Day 2
		setForecastDayWeather(2, R.id.day_two_date, R.id.day_two_conditions, R.id.day_two_image, R.id.day_two_high_temperature, R.id.day_two_low_temperature);
		
		//Day 3
		setForecastDayWeather(3, R.id.day_three_date, R.id.day_three_conditions, R.id.day_three_image, R.id.day_three_high_temperature, R.id.day_three_low_temperature);
		
		//Hide the progress bar, set the fields to visible
		hideForecastWeatherProgressBar();
		showForecastWeatherViews();
		
		return;
	}
	
	/*Set a specific forecast day's weather UI */
	private void setForecastDayWeather(int day, int dateTextResId, int conditionsTextResId, int imageWebViewResId, int highTempTextResId, int lowTempTextResId){
		
		WeatherDayInfo weatherData;
		
		//Set the proper Weather day info container
		switch(day){
		case 1:
			weatherData = weatherGatherer.dayOneWeather;
			break;
		case 2:
			weatherData = weatherGatherer.dayTwoWeather;
			break;
		case 3:
			weatherData = weatherGatherer.dayThreeWeather;
			break;
		default:
			return;
		}
		
		//Set the date text
		setTextInTextView(dateTextResId, weatherData.date);
		
		//Set the conditions text
		setTextInTextView(conditionsTextResId, weatherData.conditions);
		
		//Set the image url
		setImageUrlInWebView(imageWebViewResId, weatherData.imageUrl);
		
		//Set the high temp text in the form 59.8 F (15.4 C)
		String highTempString = createHighTempString(weatherData);
		setTextInTextView(highTempTextResId, highTempString);
		
		//Set the low temp text in the form 59.8 F (15.4 C)
		String lowTempString = createLowTempString(weatherData);
		setTextInTextView(lowTempTextResId, lowTempString);
	}
	
	/*Enters the given text into the textview pointed to by the given resource id */
	private void setTextInTextView(int resId, String text){
		TextView textView = (TextView) this.findViewById(resId);
		textView.setText(text);
	}
	
	/* Set a Webview with the url of an image */
	private void setImageUrlInWebView(int resId, String imageUrl){
		WebView imageWebView = (WebView) this.findViewById(resId);
		imageWebView.getSettings().setJavaScriptEnabled(false);
		imageWebView.loadUrl(imageUrl);
	}
	
	/* Creates string of the form 59.8 F (15.4 C) */
	private String createHighTempString(WeatherDayInfo weatherData){
		
		//Get the fahrenheit value F = [(9/5)*C] + 32
		String celsiusString = weatherData.highCelsius;
		double celsiusDouble = Double.parseDouble(celsiusString);
		double fahrenheitDouble = celsiusToFahrenheit(celsiusDouble);
		String fahrenheitString = Double.toString(fahrenheitDouble);
		
		//Put the string together
		String temperatureString = createTemperatureString(fahrenheitString, celsiusString);
		
		return temperatureString;
	}
	
	private String createLowTempString(WeatherDayInfo weatherData){
		//Get the fahrenheit value F = [(9/5)*C] + 32
		String celsiusString = weatherData.lowCelsius;
		double celsiusDouble = Double.parseDouble(celsiusString);
		double fahrenheitDouble = celsiusToFahrenheit(celsiusDouble);
		String fahrenheitString = Double.toString(fahrenheitDouble);
				
		//Put the string together
		String temperatureString = createTemperatureString(fahrenheitString, celsiusString);
				
		return temperatureString;
	}
	
	private String createTemperatureString(String fahrenheit, String celsius){
		
		//Trim the string if there are too many sig figs
		if(fahrenheit.contains(".") && fahrenheit.length() > 4){
			fahrenheit = fahrenheit.substring(0, 4);
		}
		
		//Trim the string if there are too many sig figs
		if(celsius.contains(".") && celsius.length() > 4){
			celsius = celsius.substring(0, 4);
		}
		
		String temperatureString = fahrenheit + " F (" + celsius + " C)";
		
		return temperatureString;
	}
	
	private double celsiusToFahrenheit(double celsius){
		double fahrenheit = ((9.0/5.0)*celsius) + 32.0;
		return fahrenheit;
	}
	
	private void gatherWeatherData(){
		if(weatherGatherer == null){
			weatherGatherer = new WeatherGatherer(this, WeatherLoadCompleteHandler, 98122);
		}
		
		int zipcode = getZipcodeFromEditText();
		
		if(zipcode== 0){
			return;
		}
		
		weatherGatherer.setZipcode(zipcode);
		weatherGatherer.collectData();
		
		//Show the Progress bars
		showTodayWeatherProgressBar();
		showForecastWeatherProgressBar();
		
		//hide the views
		hideTodayWeatherViews();
		hideForecastWeatherViews();
	}
	
	/* Returns the zipcode value present in the Edit Text field. Return of 0 means a non-zipcode value was entered */
	private int getZipcodeFromEditText(){
		int zipcode = 0;
		
		EditText zipcodeEditText = (EditText) this.findViewById(R.id.zipcodeEditText);
		String zipcodeString = zipcodeEditText.getText().toString();
		
		//Check for length error case
		if(zipcodeString.length() != 5){
			zipcodeEditText.setError(getString(R.string.zipcode_error));;
			return 0;
		}
		
		try{
			zipcode = Integer.parseInt(zipcodeString);
		}
		catch(NumberFormatException e){
			//Check for non-integer error case
			zipcodeEditText.setError(getString(R.string.zipcode_error));;
			return 0;
		}
		
		return zipcode;
	}
	
	private void hideTodayWeatherProgressBar(){
		//Hide Progress Bar
		ProgressBar todayWeatherProgressBar = (ProgressBar) findViewById(R.id.todayWeatherProgressBar);
		todayWeatherProgressBar.setVisibility(View.GONE);
	}
	
	private void showTodayWeatherProgressBar(){
		//Show Progress Bar
		ProgressBar todayWeatherProgressBar = (ProgressBar) findViewById(R.id.todayWeatherProgressBar);
		todayWeatherProgressBar.setVisibility(View.VISIBLE);
	}
	
	private void showTodayWeatherViews(){
		//Show fields
		LinearLayout todayWeatherLinearLayout = (LinearLayout) findViewById(R.id.todayWeatherLinearLayout);
		todayWeatherLinearLayout.setVisibility(View.VISIBLE);
	}
	
	private void hideTodayWeatherViews(){
		//Hide Fields
		LinearLayout todayWeatherLinearLayout = (LinearLayout) findViewById(R.id.todayWeatherLinearLayout);
		todayWeatherLinearLayout.setVisibility(View.GONE);
	}
	
	private void hideForecastWeatherProgressBar(){
		//Hide Progress Bar
		ProgressBar forecastWeatherProgressBar = (ProgressBar) findViewById(R.id.forecastWeatherProgressBar);
		forecastWeatherProgressBar.setVisibility(View.GONE);	
	}
	
	private void showForecastWeatherProgressBar(){
		//Show Progress Bar
		ProgressBar forecastWeatherProgressBar = (ProgressBar) findViewById(R.id.forecastWeatherProgressBar);
		forecastWeatherProgressBar.setVisibility(View.VISIBLE);
	}
	
	private void showForecastWeatherViews(){
		//Show fields
		LinearLayout forecastWeatherLinearLayout = (LinearLayout) findViewById(R.id.forecastWeatherLinearLayout);
		forecastWeatherLinearLayout.setVisibility(View.VISIBLE);
	}
	
	private  void hideForecastWeatherViews(){
		//Hide fields
		LinearLayout forecastWeatherLinearLayout = (LinearLayout) findViewById(R.id.forecastWeatherLinearLayout);
		forecastWeatherLinearLayout.setVisibility(View.GONE);
	}
	
	/* On Click action for the get forecast button */
	public void getWeatherDataOnClick(View v){
		gatherWeatherData();
	}
	
	private void setConnectivityErrorMessage(){
		//Hide progress bars
		this.hideTodayWeatherProgressBar();
		this.hideForecastWeatherProgressBar();
		
		//Hide Views
		this.hideTodayWeatherViews();
		this.hideForecastWeatherViews();
		
		//Show error toast
		Toast errorToast = Toast.makeText(this, R.string.connectivity_error, Toast.LENGTH_LONG);
		errorToast.show();
	}
	
	/* take the user to the weather data sources website*/
	public void weatherSourceOnClick(View v){
		//Open a web browser with URL
		String url = getString(R.string.data_source_url);
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(browserIntent);
	}

}
