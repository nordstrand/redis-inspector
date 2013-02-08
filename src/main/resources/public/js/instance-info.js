

function updateInstance(instance) {
    $.ajax({
        type : 'GET',
        url : '/xhr/'+instance,
        dataType : 'json',
        timeout : 500,
        context : $('body'),
        success : function(data) {                    
            $.each(data, function(key, value){
              $("#"+key).text(value);
            })
            setTimeout(function() {updateInstance(instance);}, 2000);           
        },
        error : function(xhr, type) {
            console.log('Ajax error!')
        }
    })    
}