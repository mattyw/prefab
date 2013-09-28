define(function(require) {
	"use strict";

	require('domReady!');

	$('#feed-create').on('submit', function(evt) {
		evt.preventDefault();

		var $this = $(this);

		$.ajax({
			url:  $this.attr('action'),
			type: $this.attr('method'),
			data: {
				name: $this.find('[name="Feed[name]"]').val(),
				urls: $this.find('[name="Feed[urls]"]').val().split("\n")
			}
		}).then(function(response) {
			if (response.location) {
				window.location.replace(response.location);
			}
		});
	});
});
