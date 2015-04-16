/******************************************************************
* Author: Malcolm Haynes
* Date: 4/1/2015
* 
* iOne Smart Alarm - 
* This code controls the Pebble watch app. The app will
* alarm when it receives a message from Android. It then 
* detects motion and turns off based on motion. It will turn
* back on for lack of motion after being turned off.
*******************************************************************/

#include <pebble.h>
#include "iOne.h"

static Window *window;
static TextLayer *text_layer;
static TextLayer *time_layer;
static TextLayer *date_layer;
static TextLayer *event_layer;
static TextLayer *weather_layer;
static char *alarm_set_message;
static uint16_t biggest_movement = 0;
static uint16_t shake_cnt = 0;
uint8_t wake_up_val = 0;
uint8_t get_up_val = 0;
uint8_t alarm_on_val = 0;   
bool SHOW_SHAKE = false;
bool SHOW_MOVEMENT = false;
bool SHOW_ALARM = true;

/*************************************************
 * Send Alarm On/Off Message to Android phone    *
 *************************************************/
static void alarm_message(int message, uint8_t alarm_set){
  // Begin dictionary
  DictionaryIterator *iter;
  app_message_outbox_begin(&iter);

  // Add a key-value pair
  dict_write_uint8(iter, message, alarm_set);

  // Send the message!
  if (alarm_set == 1)
      APP_LOG(APP_LOG_LEVEL_INFO, "Sending 1");
  else 
      APP_LOG(APP_LOG_LEVEL_INFO, "Sending 0");

  app_message_outbox_send();
}

/****************************************************
 * Sound gradual alarm. Modified from Morpheuz app
 *by James Fowler           
 ****************************************************/
// Times (in seconds) between each buzz (gives a progressive alarm and gaps between phases)
static uint8_t alarm_pattern[] = { 5, 4, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

static uint8_t alarm_count;
static AppTimer *alarm_timer;

#define ALARM_LIMIT (ARRAY_LENGTH(alarm_pattern) * ALARM_PATTERN_MAX_REPEAT)

static void gradual_alarm(void *data) {
  // Already hit the limit so let them sleep some more
  if (alarm_count >= ALARM_LIMIT) {
    return;
  }

  // Vibrate
  if (biggest_movement > MOVEMENT_THRESHOLD){
    if (alarm_count < 10)
      vibes_short_pulse();
    else
      vibes_long_pulse();
  }

  if (biggest_movement > SHAKE_THRESHOLD)
    biggest_movement = 0;
  
  if (shake_cnt < NUM_SHAKES_TO_TURNOFF && wake_up_val == 1)
    // If not turned off, then rinse and repeat
    alarm_timer = app_timer_register(((uint16_t) alarm_pattern[alarm_count % (ARRAY_LENGTH(alarm_pattern))]) * 1000, gradual_alarm, NULL);
  else{
    biggest_movement = 0;
    alarm_message(ALARM_ON, 0); 
    alarm_on_val = 0;
    wake_up_val = 0;
  }
  alarm_count++;
  
}

/*********************************
 *   Sound getup alarm           *
 *********************************/
static void getup_alarm(void *data) {
  //Create an array of ON-OFF-ON etc durations in milliseconds
  uint32_t getup_segments[] = {850};

  //Create a VibePattern structure with the segments and length of the pattern as fields
  VibePattern pattern = {
     .durations = getup_segments,
     .num_segments = ARRAY_LENGTH(getup_segments), 
  };

  //Trigger the custom pattern to be executed
  vibes_enqueue_custom_pattern(pattern);
  
  if (biggest_movement > SHAKE_THRESHOLD)
    biggest_movement = 0;
    
  if (shake_cnt < NUM_SHAKES_TO_TURNOFF && get_up_val == 1)
    // Prepare the time for the next buzz (this gives progressing and phasing)
    alarm_timer = app_timer_register(1000, getup_alarm, NULL);
  else{
    biggest_movement = 0;
    alarm_message(ALARM_ON, 0);
    alarm_on_val = 0;
    get_up_val = 0;
  }
}

/******************************************************************
 * Scale accelerometer data so that always positive. Modified from
 * Morpheuz app by James Fowler           
 *****************************************************************/
static uint16_t scale_accel(int16_t val) {
  int16_t retval = 4000 + val;
  if (retval < 0)
    retval = 0;
  return retval;
}

/*****************************************************************
 * Find accelerometer biggest normalized deviation. Modified from 
 * Morpheuz app by James Fowler                                  
 *****************************************************************/
static void do_axis(int16_t val, uint16_t *biggest, uint32_t avg) {
  uint16_t val_scale = scale_accel(val);
  if (val_scale < avg)
    val_scale = avg - val_scale;
  else
    val_scale -= avg;
  if (val_scale > *biggest)
    *biggest = val_scale;
}

/*****************************************************************
 * Process accelerometer data. Modified from Morpheuz app
 * by James Fowler
 *****************************************************************/
static void accel_data_handler(AccelData *data, uint32_t num_samples) {
  // Average the data
  uint32_t avg_x = 0;
  uint32_t avg_y = 0;
  uint32_t avg_z = 0;
  AccelData *dx = data;
  
  for (uint32_t i = 0; i < num_samples; i++, dx++) {
    // If vibe went off then discount everything
    if (dx->did_vibrate) {
      return;
    }
    avg_x += scale_accel(dx->x);
    avg_y += scale_accel(dx->y);
    avg_z += scale_accel(dx->z);
  }

  avg_x /= num_samples;
  avg_y /= num_samples;
  avg_z /= num_samples;

  // Work out deviations
  uint16_t biggest = 0;
  AccelData *d = data;
  for (uint32_t i = 0; i < num_samples; i++, d++) {
    do_axis(d->x, &biggest, avg_x);
    do_axis(d->y, &biggest, avg_y);
    do_axis(d->z, &biggest, avg_z);
  }

  //check to see if movement bigger than current biggest
  static char s_buffer[128];
  if (biggest > biggest_movement)
    biggest_movement = biggest;
    
  //check to see if they shook hard
  if (biggest > SHAKE_THRESHOLD)
    shake_cnt += 1;
    
  //Show different info on bottom based on which button user pressed
  if (SHOW_SHAKE)
      snprintf(s_buffer, sizeof(s_buffer), "Shakes: %lu", (unsigned long)shake_cnt);
  if (SHOW_MOVEMENT)
      snprintf(s_buffer, sizeof(s_buffer), "Movement: %lu", (unsigned long)biggest);
  if (SHOW_SHAKE || SHOW_MOVEMENT){
      text_layer_set_text(text_layer, s_buffer);
  }
  if (SHOW_ALARM){
     if (get_up_val == 1 || wake_up_val == 1){
        int countdown = NUM_SHAKES_TO_TURNOFF - shake_cnt;
        if (countdown >= 0)
            snprintf(s_buffer,sizeof(s_buffer),"Shake To End: %lu",(unsigned long) countdown);
     }
     else
        snprintf(s_buffer, sizeof(s_buffer), "%s", alarm_set_message);
    text_layer_set_text(text_layer, s_buffer);
  }
}

/*********************************
 * Display Current Time          *
 *********************************/
static void update_time() {
  // Get a tm structure
  time_t temp = time(NULL); 
  struct tm *tick_time = localtime(&temp);

  // Create a long-lived buffer
  static char buffer[] = "00:00";
  static char date_buffer[90];


  // Write the current hours and minutes into the buffer
  if(clock_is_24h_style() == true) {
    // Use 24 hour format
    strftime(buffer, sizeof("00:00"), "%H:%M", tick_time);
  } else {
    // Use 12 hour format
    strftime(buffer, sizeof("00:00"), "%I:%M", tick_time);
  }

  // Display this time on the TextLayer
  text_layer_set_text(time_layer, buffer);
  
  //Display this date on the DateLayer
  strftime(date_buffer, sizeof(date_buffer), "%A, %b %e", tick_time);
  text_layer_set_text(date_layer, date_buffer);
}

/*********************************
 * Minute Tick Handler           *
 *********************************/
static void handle_minute_tick(struct tm *tick_time, TimeUnits units_changed) {
    update_time();
    
    //checks for movement and triggers alarm if no movement after so many minutes
    if(tick_time->tm_min%MINUTES_NO_MOVEMENT==0 && get_up_val==1 && alarm_on_val==0) {
      if (biggest_movement < MOVEMENT_THRESHOLD)
        getup_alarm(NULL);
      biggest_movement = 0;
    }
}

/*********************************************************************/
/*-------------------------------------------------------------------*/
/*                      App Communication Handlers                   */
/*-------------------------------------------------------------------*/
/*********************************************************************/
 
 /*********************************
 *         Inbox handler          *
 **********************************/
static void inbox_received_callback(DictionaryIterator *iterator, void *context) {
  // Read first item
  Tuple *t = dict_read_first(iterator);
  static char s_buffer[128];
  static char w_buffer[128];
  static char e_buffer[128];


  // For all items
  while(t != NULL) {
    // Which key was received?
    switch(t->key) {
      case WAKE_UP:
        APP_LOG(APP_LOG_LEVEL_INFO, "Received Wakeup Key");
        wake_up_val = t->value->uint8;
        if (wake_up_val == 1){
          get_up_val = 0;
          biggest_movement = 0;
          shake_cnt = 0;
          alarm_message(ALARM_ON, (uint8_t) 1);
          alarm_on_val = 1;
          gradual_alarm(NULL);
        }
        break;
      case GET_UP:
        APP_LOG(APP_LOG_LEVEL_INFO, "Received Getup Key");
        get_up_val = t->value->uint8;
        if (get_up_val == 1){
          wake_up_val = 0;
          biggest_movement = 0;
          shake_cnt = 0;
          alarm_message(ALARM_ON, (uint8_t) 1);
          alarm_on_val = 1;
          getup_alarm(NULL);
        }
        break;
      case CALENDAR:
        APP_LOG(APP_LOG_LEVEL_INFO, "Received Calendar Key");
        snprintf(s_buffer, sizeof(s_buffer), "Event: %s", t->value->cstring);
        //Show the data
        text_layer_set_text(event_layer, s_buffer);
        break;
      case WEATHER:
        APP_LOG(APP_LOG_LEVEL_INFO, "Received Weather Key");
        snprintf(w_buffer, sizeof(w_buffer), "%s", t->value->cstring);
        text_layer_set_text(weather_layer, w_buffer);
        break;
      case ALARM_SET_TIME:
        APP_LOG(APP_LOG_LEVEL_INFO, "Received Alarm Set Key");
        snprintf(e_buffer, sizeof(e_buffer), "%s", t->value->cstring);
        text_layer_set_text(text_layer, e_buffer);
        alarm_set_message = t->value->cstring;
        break;
      case APP_READY:
        APP_LOG(APP_LOG_LEVEL_INFO, "App Ready Received");
        alarm_message(APP_READY, 1);
        break;
      case ALARM_ON:
        APP_LOG(APP_LOG_LEVEL_INFO, "Why the hell you sending me Alarm On Key?");
        break;
      default:
        APP_LOG(APP_LOG_LEVEL_ERROR, "Key %d not recognized!", (int)t->key);
        break;
    }
    // Look for next item
    t = dict_read_next(iterator);
  }
}

 /*********************************
 *      Inbox drop handler        *
 **********************************/
static void inbox_dropped_callback(AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped!");
}

 /*********************************
 *    Outbbox drop handler        *
 **********************************/
static void outbox_failed_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox send failed!");
}

 /*********************************
 *         Outbox handler          *
 **********************************/
static void outbox_sent_callback(DictionaryIterator *iterator, void *context) {
  APP_LOG(APP_LOG_LEVEL_INFO, "Outbox send success!");
}


/*********************************************************************/
/*-------------------------------------------------------------------*/
/*                       Button Click Handlers                       */
/*-------------------------------------------------------------------*/
/*********************************************************************/

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
  SHOW_SHAKE = false;
  SHOW_MOVEMENT = false;
  SHOW_ALARM = true;
}

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
  SHOW_SHAKE = true;
  SHOW_MOVEMENT = false;
  SHOW_ALARM = false;
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  SHOW_SHAKE = false;
  SHOW_MOVEMENT = true;
  SHOW_ALARM = false;
}

static void back_click_handler(ClickRecognizerRef recognizer, void *context) {
    alarm_message(DEMO_WAKEUP, 1);
}

static void  multi_click_handler(ClickRecognizerRef recognizer, void *context) {
    alarm_message(DEMO_GETUP, 1);
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
  window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
  window_single_click_subscribe(BUTTON_ID_BACK, back_click_handler);
  window_multi_click_subscribe(BUTTON_ID_SELECT, 2, 10, 0, true, multi_click_handler);

}

/*********************************************************************/
/*-------------------------------------------------------------------*/
/*                      Mandatory Pebble App Setup                   */
/*-------------------------------------------------------------------*/
/*********************************************************************/

static void window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);

  //Might as well show time to be more useful
  time_layer = text_layer_create(GRect(0, 10, 144, 35));
  text_layer_set_text(time_layer, "00:00");
  text_layer_set_text_color(time_layer, GColorBlack);
  text_layer_set_font(time_layer, fonts_get_system_font(FONT_KEY_BITHAM_30_BLACK));
  text_layer_set_text_alignment(time_layer, GTextAlignmentCenter);
  // Add it as a child layer to the Window's root layer
  layer_add_child(window_layer, text_layer_get_layer(time_layer));
  
  //Add date as well
  date_layer = text_layer_create(GRect(0, 40, 144, 40));
  text_layer_set_text(date_layer, "Loading Date...");
  text_layer_set_text_color(date_layer, GColorBlack);
  text_layer_set_font(date_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24));
  text_layer_set_text_alignment(date_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(date_layer));

  //Add weather as well
  weather_layer = text_layer_create(GRect(0, 70, 144, 25));
  text_layer_set_text(weather_layer, "Loading Weather...");
  text_layer_set_background_color(weather_layer, GColorBlack);
  text_layer_set_text_color(weather_layer, GColorWhite);
  text_layer_set_font(weather_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
  text_layer_set_text_alignment(weather_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(weather_layer));

  //Add event as well
  event_layer = text_layer_create(GRect(0, 92, 144, 25));
  text_layer_set_text(event_layer, "Loading Event...");
  text_layer_set_background_color(event_layer, GColorBlack);
  text_layer_set_text_color(event_layer, GColorWhite);
  text_layer_set_font(event_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
  text_layer_set_text_alignment(event_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(event_layer));

    
  // Show varying information here
  text_layer = text_layer_create(GRect(0, 130, 144, 30));
  alarm_set_message = "iOne Smart Alarm";
  text_layer_set_text(text_layer, alarm_set_message);
  text_layer_set_font(text_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24));
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(text_layer));
  
  alarm_message(APP_READY, 1); 
}

static void window_unload(Window *window) {
  text_layer_destroy(text_layer);
  text_layer_destroy(date_layer); 
  text_layer_destroy(time_layer);
}

static void init(void) {
  window = window_create();
  window_set_fullscreen(window, true);
  window_set_click_config_provider(window, click_config_provider);
  window_set_window_handlers(window, (WindowHandlers) {
    .load = window_load,
    .unload = window_unload,
  });
  const bool animated = true;
  window_stack_push(window, animated);
  
  // Subscribe to the accelerometer data service
  accel_data_service_subscribe(25, accel_data_handler);

  // Choose update rate
  accel_service_set_sampling_rate(ACCEL_SAMPLING_25HZ);
  
  // Subscribe to minute level tick events
  tick_timer_service_subscribe(MINUTE_UNIT, handle_minute_tick);
  
  update_time();
  
  // Register Android app callbacks
  app_message_register_inbox_received(inbox_received_callback);
  app_message_register_inbox_dropped(inbox_dropped_callback);
  app_message_register_outbox_failed(outbox_failed_callback);
  app_message_register_outbox_sent(outbox_sent_callback);
  
  // Open AppMessage
 app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());
}

static void deinit(void) {
  window_destroy(window);
}

int main(void) {
  init();

  APP_LOG(APP_LOG_LEVEL_DEBUG, "Done initializing, pushed window: %p", window);

  app_event_loop();
  deinit();
}
