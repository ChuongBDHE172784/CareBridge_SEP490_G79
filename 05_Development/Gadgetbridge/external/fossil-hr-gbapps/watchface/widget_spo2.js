return {
    node_name: '',
    manifest: {
        timers: [],
    },
    init: undefined,
    deinit: undefined,
    handler: undefined,
    persist: {},
    config: {},
    last_value: '– –',
    handler: function (event, response) {
        if (event.type == 'watch_face_update') {
            this.draw(response, true);
        } else {
            if (event.type == 'display_data_updated') {
                this.draw(response, true);
            } else {
                if (event.type == 'common_update' && event.device_offwrist) {
                    this.draw(response, true);
                }
            }
        }
    },
    draw: function (response, redraw_needed) {
        if (get_common().device_offwrist == false && get_common().U('HRM') == 'ON' && get_common().spo2_level > 0) {
            if (this.last_value != get_common().spo2_level) {
                this.last_value = get_common().spo2_level;
                redraw_needed = true;
            }
        } else {
            this.last_value = '– –';
        }
        if (redraw_needed) {
            response.draw = {};
            response.draw[this.node_name] = {
                json_file: 'complication_layout',
                icon: 'icSpO2',
                ci: this.last_value,
            };
        }
    },
};
