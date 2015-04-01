#include <pebble.h>
#include "iOne.h"

static Window *window;
static TextLayer *text_layer;
static TextLayer *time_layer;
static TextLayer *date_layer;
static uint16_t biggest_movement = 0;
static uint16_t shake_cnt = 0;
uint8_t wake_up_val = 0;
uint8_t get_up_val = 0;
uint8_t alarm_on_val = 0;   

/*********************************
 * Send Alarm On/Off Message     *
 *********************************/
static void alarm_message(uint8_t alarm_set){
  // Begin dictionary
  DictionaryIterator *iter;
  app_message_outbox_begin(&iter);

  // Add a key-value pair
  dict_write_uint8(iter, ALARM_ON, alarm_set);

  // Send the message!
  app_message_outbox_send();
}

/*********************************
 * Sound gradual alarm           *
 *********************************/
// Times (in seconds) between each buzz (gives a progressive alarm and gaps between phases)
static uint8_t alarm_pattern[] = { 5, 4, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
//Create an array of ON-OFF-ON etc durations in milliseconds

static uint8_t alarm_count;
static AppTimer *alarm_timer;

#define ALARM_LIMIT (ARRAY_LENGTH(alarm_pattern) * ALARM_PATTERN_MAX_REPEAT)

static void gradual_alarm(void *data) {
  alarm_message((uint8_t) 1);
  alarm_on_val = 1;

  // Already hit the limit
  if (alarm_count >= ALARM_LIMIT) {
    return;
  }

  // Vibrate
  if (alarm_count < 10)
    vibes_short_pulse();
  else
    vibes_long_pulse();

  if (biggest_movement > SHAKE_THRESHOLD)
    biggest_movement = 0;
  
  if (shake_cnt < NUM_SHAKES_TO_TURNOFF && wake_up_val == 1)
    // Prepare the time for the next buzz (this gives progressing and phasing)
    alarm_timer = app_timer_register(((uint16_t) alarm_pattern[alarm_count % (ARRAY_LENGTH(alarm_pattern))]) * 1000, gradual_alarm, NULL);
  else
    biggest_movement = 0;
    alarm_message((uint8_t) 0); 
    alarm_on_val = 0;

  alarm_count++;
  
}

/*********************************
 *   Sound getup alarm           *
 *********************************/
static void getup_alarm(void *data) {
  alarm_message((uint8_t) 1);
  alarm_on_val = 1;

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
  else
    biggest_movement = 0;
    alarm_message((uint8_t) 0);
    alarm_on_val = 0;
    
}

/*****************************************
 * Scale data so that always positive    *
 *****************************************/
static uint16_t scale_accel(int16_t val) {
  int16_t retval = 4000 + val;
  if (retval < 0)
    retval = 0;
  return retval;
}

/*******************************************
 * Find biggest normalized deviation       *
 *******************************************/
static void do_axis(int16_t val, uint16_t *biggest, uint32_t avg) {
  uint16_t val_scale = scale_accel(val);
  if (val_scale < avg)
    val_scale = avg - val_scale;
  else
    val_scale -= avg;
  if (val_scale > *biggest)
    *biggest = val_scale;
}

/*********************************
 * Process accelerometer data    *
 *********************************/
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

//   store_sample(biggest);
  // Long lived buffer
  static char s_buffer[128];

  if (biggest > biggest_movement)
    biggest_movement = biggest;
    
  if (biggest > SHAKE_THRESHOLD)
    shake_cnt += 1;
    
  // Compose string of all data
  snprintf(s_buffer, sizeof(s_buffer), 
    "X: %lu\nY: %lu\nZ: %lu\nBiggest: %lu\nShakes: %lu", 
    (unsigned long)avg_x, (unsigned long)avg_y, (unsigned long)avg_z, (unsigned long)biggest, (unsigned long)shake_cnt);

  //Show the data
  text_layer_set_text(text_layer, s_buffer);
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
  static char date_buffer[20];


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
  
  //Display this time on the DateLayer
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

/*********************************
 * App Communication Functions   *
 *********************************/
static void inbox_received_callback(DictionaryIterator *iterator, void *context) {
  // Read first item
  Tuple *t = dict_read_first(iterator);
  static char s_buffer[128];

  // For all items
  while(t != NULL) {
    // Which key was received?
    switch(t->key) {
      case WAKE_UP:
        wake_up_val = t->value->uint8;
        if (wake_up_val == 1){
          biggest_movement = 0;
          shake_cnt = 0;
          gradual_alarm(NULL);
        }
        break;
      case GET_UP:
        get_up_val = t->value->uint8;
        if (get_up_val == 1){
          biggest_movement = 0;
          shake_cnt = 0;
          getup_alarm(NULL);
        }
        break;
      case CALENDAR:
        snprintf(s_buffer, sizeof(s_buffer), "First Event: %s", t->value->cstring);
        //Show the data
        text_layer_set_text(text_layer, s_buffer);
      case ALARM_ON:
        break;
      default:
        APP_LOG(APP_LOG_LEVEL_ERROR, "Key %d not recognized!", (int)t->key);
        break;
    }
    // Look for next item
    t = dict_read_next(iterator);
  }
}

static void inbox_dropped_callback(AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped!");
}

static void outbox_failed_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox send failed!");
}

static void outbox_sent_callback(DictionaryIterator *iterator, void *context) {
  APP_LOG(APP_LOG_LEVEL_INFO, "Outbox send success!");
}

/*********************************
 * Standard Pebble App Setup     *
 *********************************/
static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Select");
}

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Up");
  biggest_movement = 0;
  shake_cnt = 0;
  gradual_alarm(NULL);
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Down");
  biggest_movement = 0;
  shake_cnt = 0;
  getup_alarm(NULL);
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
  window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}
static void window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);

  //Might as well show time to be more useful
  time_layer = text_layer_create(GRect(0, 90, 144, 20));
  text_layer_set_background_color(time_layer, GColorBlack);
  text_layer_set_text_color(time_layer, GColorWhite);
//   text_layer_set_text(time_layer, "00:00");
  text_layer_set_text_alignment(time_layer, GTextAlignmentCenter);
  // Add it as a child layer to the Window's root layer
  layer_add_child(window_layer, text_layer_get_layer(time_layer));
  
  //Add date as well
  date_layer = text_layer_create(GRect(0, 115, 144, 20));
  text_layer_set_background_color(date_layer, GColorBlack);
  text_layer_set_text_color(date_layer, GColorWhite);
  text_layer_set_text_alignment(date_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(date_layer));
    
  // Currently showing accelorometer data
  text_layer = text_layer_create(GRect(5, 5, 139, 75));
  text_layer_set_text(text_layer, "Press a button");
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(text_layer));
  
}

static void window_unload(Window *window) {
  text_layer_destroy(text_layer);
  text_layer_destroy(date_layer); 
  text_layer_destroy(time_layer);
}

static void init(void) {
  window = window_create();
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
