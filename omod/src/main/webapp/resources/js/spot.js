var $JQuery = jQuery.noConflict();
$JQuery("#spotInfo").submit(function(event) { 
	$JQuery("#loading").show();			
			event.preventDefault();			
			var url = "/openmrs/ws/rest/v1/clinic/spot/save";			
			var token = $JQuery("meta[name='_csrf']").attr("content");
			var header = $JQuery("meta[name='_csrf_header']").attr("content");
			var psiClinicManagementId = $JQuery('input[name=psiClinicManagement]').val();
			var formData;			
			formData = {
			            'name': $JQuery('input[name=name]').val(),			           
			            'code': $JQuery('input[name=code]').val(),			           
			            'address': $JQuery('input[name=address]').val(),
			            'dhisId': $JQuery('input[name=dhisId]').val(),
			            'csid': $JQuery('input[name=ccsid]').val(),
			            'psiClinicManagementId': psiClinicManagementId
			 };
			
			$JQuery.ajax({
				contentType : "application/json",
				type: "POST",
		        url: url,
		        data: JSON.stringify(formData), 
		        dataType : 'json',		        
				timeout : 100000,
				beforeSend: function(xhr) {				    
					 xhr.setRequestHeader(header, token);
				},
				success : function(data) {
					$JQuery("#usernameUniqueErrorMessage").html(data);
					$JQuery("#loading").hide();
				   if(data == ""){					   
					   window.location.replace("/openmrs/module/PSI/PSIClinicSpotList.form?id="+psiClinicManagementId);
				   }
				   
				},
				error : function(e) {
				   
				},
				done : function(e) {				    
				    console.log("DONE");				    
				}
			}); 
		});

