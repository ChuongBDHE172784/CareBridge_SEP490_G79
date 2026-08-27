return {
    node_name: '',
    manifest: {
        timers: ['flick_away', 'exit_timer'],
    },
    persist: {},
    config: {
        info: {
            distance: undefined,
            eta: undefined,
            instruction: undefined,
            nextAction: undefined,
            autoFg: true,
            vibrate: true,
        },
    },
    locked: false,
    time_telling: false,
    prev_instruction: '',
    handler: function (event, response) {
        this.wrap_event(event);
        this.wrap_response(response);
        this.state_machine.handle_event(event, response);
    },
    log: function (object) {
        req_data(
            this.node_name,
            '"type": "log", "node":"' + this.node_name + '", "tag":"", "data":' + JSON.stringify(object),
            999999,
            true
        );
    },
    wrap_event: function (system_state_update_event) {
        if (system_state_update_event.type === 'system_state_update') {
            system_state_update_event.concerns_this_app = system_state_update_event.de;
            system_state_update_event.old_state = system_state_update_event.ze;
            system_state_update_event.new_state = system_state_update_event.le;
        }
        return system_state_update_event;
    },
    wrap_response: function (response) {
        response.move_hands = function (degrees_hour, degrees_minute, relative) {
            response.move = {
                h: degrees_hour,
                m: degrees_minute,
                is_relative: relative,
            };
        };
        response.vibrate_pattern = function (type) {
            response.vibe = type;
        };
        response.vibrate_text_pattern = function () {
            this.vibrate_pattern('text');
        };
        response.vibrate_call_pattern = function () {
            this.vibrate_pattern('call');
        };
        response.go_back = function (kill_app) {
            response.action = {
                type: 'go_back',
                Se: kill_app,
            };
        };
        response.go_home = function (kill_app) {
            response.action = {
                type: 'go_home',
                Se: kill_app,
            };
        };
        response.close_app = function () {
            response.action = {
                type: 'close_app',
                class: 'watch_app',
            };
            this.state_machine.stop();
        };
        response.draw_screen = function (node_name, full_update, layout_info) {
            response.draw = {
                update_type: full_update ? 'gc4' : 'du4',
            };
            response.draw[node_name] = {
                layout_function: 'layout_parser_json',
                layout_info: layout_info,
            };
        };
        response.send_user_class_event = function (event_type) {
            response.send_generic_event({
                type: event_type,
                class: 'user',
            });
        };
        response.emulate_double_tap = function () {
            this.send_user_class_event('double_tap');
        };
        response.send_generic_event = function (event_object) {
            if (response.i == undefined) response.i = [];
            response.i.push(event_object);
        };
        response.open_app = function (appName) {
            response.action = {
                type: 'open_app',
                node_name: appName,
                class: 'watch_app',
            };
        };
        response.go_visible = function () {
            response.action = {
                type: 'go_visible',
                class: 'alert',
            };
        };
        return response;
    },
    wrap_state_machine: function (state_machine) {
        state_machine.set_current_state = state_machine.d;
        state_machine.handle_event = state_machine._;
        state_machine.stop = state_machine.c;
        state_machine.get_current_state = function () {
            return state_machine.n;
        };

        return state_machine;
    },
    handle_global_event: function (self, state_machine, event, response) {
        if (event.type === 'system_state_update' && event.concerns_this_app === true) {
            if (event.new_state === 'visible') {
                self.state_machine.set_current_state('navigating');
            } else {
                self.state_machine.set_current_state('background');
            }
        } else if (event.type == 'node_config_update' && event.node_name == self.node_name) {
            if (self.state_machine.get_current_state() == 'background') {
                if (self.config.info.autoFg) response.go_visible();
            } else {
                self.draw_nav_screen(response, false);
            }
        } else if (event.type === 'middle_hold') {
            response.go_home(true);
        } else if (
            !self.locked &&
            event.type == 'timer_expired' &&
            is_this_timer_expired(event, self.node_name, 'exit_timer')
        ) {
            response.close_app();
        }
    },
    handle_state_specific_event: function (state, state_phase) {
        switch (state) {
            case 'navigating': {
                if (state_phase == 'entry') {
                    return function (self, response) {
                        response.move_hands(180, 180, false);
                        self.draw_nav_screen(response, true);
                    };
                }
                if (state_phase == 'during') {
                    return function (self, state_machine, event, response) {
                        if (event.type === 'middle_short_press_release') {
                            response.go_home(true);
                        } else if (event.type === 'bottom_short_press_release') {
                            self.locked = true;
                            self.time_telling = true;
                            stop_timer(self.node_name, 'exit_timer');
                            var hands = enable_time_telling();
                            response.move_hands(hands.hour_pos, hands.minute_pos, false);
                            self.draw_nav_screen(response, false);
                        } else if (self.locked && self.time_telling && event.type == 'time_telling_update') {
                            var hands = enable_time_telling();
                            response.move_hands(hands.hour_pos, hands.minute_pos, false);
                        } else if (self.locked && event.type == 'flick_away') {
                            start_timer(self.node_name, 'flick_away', 2200);
                            self.time_telling = false;
                            disable_time_telling();
                            response.move = {
                                h: 360,
                                m: -360,
                                is_relative: true,
                            };
                        } else if (
                            event.type == 'timer_expired' &&
                            is_this_timer_expired(event, self.node_name, 'flick_away')
                        ) {
                            var hands = enable_time_telling();
                            response.move_hands(hands.hour_pos, hands.minute_pos, false);
                            self.time_telling = true;
                        }
                    };
                }
                if (state_phase == 'exit') {
                    return function (arg, arg2) {};
                }
                break;
            }
        }
    },
    draw_nav_screen: function (response, full_redraw) {
        if (!this.locked) {
            stop_timer(this.node_name, 'exit_timer');
            start_timer(this.node_name, 'exit_timer', 60000);
        }
        if (this.config.info.instruction != this.prev_instruction) {
            if (this.config.info.vibrate) response.vibrate_text_pattern();
            this.prev_instruction = this.config.info.instruction;
        }
        switch (this.config.info.nextAction) {
            case 1:
                image_direction = 'nav_continue.rle';
                break;
            case 2:
                image_direction = 'nav_turn_left.rle';
                break;
            case 3:
                image_direction = 'nav_turn_slight_left.rle';
                break;
            case 4:
                image_direction = 'nav_turn_sharp_left.rle';
                break;
            case 5:
                image_direction = 'nav_turn_right.rle';
                break;
            case 6:
                image_direction = 'nav_turn_slight_right.rle';
                break;
            case 7:
                image_direction = 'nav_turn_sharp_right.rle';
                break;
            case 8:
                image_direction = 'nav_keep_left.rle';
                break;
            case 9:
                image_direction = 'nav_keep_right.rle';
                break;
            case 10:
                image_direction = 'nav_uturn_left.rle';
                break;
            case 11:
                image_direction = 'nav_uturn_right.rle';
                break;
            case 12:
                image_direction = 'nav_off_route.rle';
                break;
            case 13:
                image_direction = 'nav_roundabout_right.rle';
                break;
            case 14:
                image_direction = 'nav_roundabout_left.rle';
                break;
            case 15:
                image_direction = 'nav_roundabout_straight.rle';
                break;
            case 16:
                image_direction = 'nav_roundabout_uturn.rle';
                break;
            case 17:
                image_direction = 'nav_finish.rle';
                break;
            case 18:
                image_direction = 'nav_merge.rle';
                break;
            default:
                image_direction = 'nav_unknown.rle';
                break;
        }
        response.draw_screen(this.node_name, full_redraw, {
            json_file: 'nav_layout',
            distance: this.config.info.distance != undefined ? this.config.info.distance : '- - -',
            image: image_direction,
            button_bottom: this.locked ? '' : 'icLock',
        });
    },
    init: function () {
        this.state_machine = new state_machine(
            this,
            this.handle_global_event,
            this.handle_state_specific_event,
            undefined,
            'background'
        );
        this.wrap_state_machine(this.state_machine);
    },
};
