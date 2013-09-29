define(function(require) {
	"use strict";

	require('domReady!');

	var form = $('#feed-create');

	form.on('submit', function(evt) {
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

	form.find('[name="Feed[name]"]').on('input', function(evt) {
		var input = $(evt.target);

		$.ajax({
			url: '/feed-name-exists',
			type: 'GET',
			data: {
				name: input.val()
			}
		}).then(function(response) {
			var taken = (response === 'true');

			input.closest('.form-group').toggleClass('has-error', taken);

			if (input.get(0).setCustomValidity) {
				input.get(0).setCustomValidity(taken ? "That feed name is already taken" : '');
			}
		});
	});
});
