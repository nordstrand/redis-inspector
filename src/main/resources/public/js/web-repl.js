var ws = null;



$(document).ready(function () {
    /*
    ws = $.websocket("ws://" + window.location.host + "/websocket", {
        events: {
            'web-repl-response': function(info) {
                currentCallback([
                    {msg: info.response,
                        className:"jquery-console-message-value"}
                ]);
            }
        }
    });
    */

    $("#console").console({
        promptLabel: '=> ',
        commandValidate:function(line) {
            if (line == "") {
                return false;
            }
            else {
                return true;
            }
        },
        commandHandle:function(line, callback) {
            currentCallback = callback;
            
            $("#text").val(line);
            
            $.ajax({
                url : "/xhr/repl", 
                type: "POST",
                data:  $("#doh").serialize(), // {name: 'text', value: line}, //$("#doh").serialize(),
                success : function(info) { 
                    currentCallback([
                                     {msg: info,
                                      className:"jquery-console-message-value"}
                                 ]);
                }       
              });

            //ws.send('selfish', {type: "web-repl", command: line});
        },
        welcomeMessage:'(redis [& body]) evaluates body with Redis commands bound to current redis instance.',
        autofocus:true,
        animateScroll:true,
        promptHistory:true
    })
});
