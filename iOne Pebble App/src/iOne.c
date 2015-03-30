#include <pebble.h>
#include "morpheuz.h"

/* Function do_alarm modified from the morpheuz source code (alarm.c)*/
static Window *window;
static TextLayer *text_layer;
static TextLayer *time_layer;
static TextLayer *date_layer;

/************************************************************************************/
/************************************************************************************/
/************************************************************************************/
/*  SO WE NEED COMMS HERE. FUNCTION 1 - CHECK FOR MESSAGES, AND ACT; FUNCTION 2 - SEND MESSAGES BASED ON MOVEMENT*/
/************************************************************************************/
/************************************************************************************/
/************************************************************************************/


/************************************************************************************/
/************************************************************************************/
/************************************************************************************/
/*  SOMEWHERE NEED TO CHECK IF BIGGEST > THRESHOLD AND AN WAKEUP ALARM IS TRUE THEN
SOUND THE GRADUAL ALARM. IF GETUP ALARM IS TRUE SOUND THE GETUP ALARM. NEED TO ADD ROUTINE TO TURN OFF ALARM BASED ON SHAKE. ALSO ROUTING TO TURN ALARM BACK ON IF WAKEUP AND BIGGEST UNDER SOME THRESHOLD FOR SOME PERIOD TIME (5 MINUTES)?.
/************************************************************************************/
/************************************************************************************/
/************************************************************************************/


/*********************************
 * Sound gradual alarm           *
 *********************************/
 
// Times (in seconds) between each buzz (gives a progressive alarm and gaps between phases)
static uint8_t alarm_pattern[] = { 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 60 };

static uint8_t alarm_count;
static AppTimer *alarm_timer;

#define ALARM_LIMIT (ARRAY_LENGTH(alarm_pattern) * ALARM_PATTERN_MAX_REPEAT)

static void do_alarm(void *data) {

  // Already hit the limit
  if (alarm_count >= ALARM_LIMIT) {
    return;
  }

  // Vibrate
  if (alarm_count < 10)
    vibes_short_pulse();
  else
    vibes_long_pulse();

  // Prepare the time for the next buzz (this gives progressing and phasing)
  alarm_timer = app_timer_register(((uint16_t) alarm_pattern[alarm_count % (ARRAY_LENGTH(alarm_pattern))]) * 1000, do_alarm, NULL);

  alarm_count++;
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
    // If vibe went off then discount everything - we're only loosing a 2.5 second set of samples, better than an
    // unwanted spike
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

  // Compose string of all data
  snprintf(s_buffer, sizeof(s_buffer), 
    "X: %lu\nY: %lu\nZ: %lu\nBiggest: %lu\n", 
    (unsigned long)avg_x, (unsigned long)avg_y, (unsigned long)avg_z, (unsigned long)biggest
  );
/************************************************************************************/
/************************************************************************************/
  /************************************************************************************/
/*  SO HERE YOU SHOULD SEND INFO TO SOME WHERE? IDEA IS TO HAVE THE TICK EVENT HANDLER CHECK THE BIGGEST VARIABLE AND DETERMINE IF MOVEMENT. EXAMINE STORE MOVEMENT*/
/************************************************************************************/
/************************************************************************************/
/************************************************************************************/

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
}

/*********************************
 * Standard Pebble App Setup     *
 *********************************/
static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Select");
}

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Up");
  do_alarm(NULL);
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Down");
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
  window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}

static void window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);
//   GRect bounds = layer_get_bounds(window_layer);

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
//   text_layer = text_layer_create((GRect) { .origin = { 0, 72 }, .size = { bounds.size.w, 20 } });
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
  accel_service_set_sampling_rate(ACCEL_SAMPLING_10HZ);
  
  // Subscribe to minute level tick events
  tick_timer_service_subscribe(MINUTE_UNIT, handle_minute_tick);
  
  update_time();
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
