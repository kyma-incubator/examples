const Sentiment = require('sentiment');
const sentiment = new Sentiment();
const axios = require("axios");

module.exports = {
    main: async function (event, context) {
        let status = "hold";
        let comment = await getComment(event.data.commentId);
        let result = sentiment.analyze(comment.content.raw);
        let score = result.comparative;
        if (score>0.2) {
            status = "approved"
        } 
        updateComment(comment.id, status, comment.content.raw, score);
    }
};

async function getComment(id) {
    let commentUrl = `${process.env.WP_GATEWAY_URL}/wp/v2/comments/${id}?context=edit`
    let response = await axios.get(commentUrl);
    return response.data;
}

async function updateComment(id, status, comment, score) {
    let commentUrl = `${process.env.WP_GATEWAY_URL}/wp/v2/comments/${id}`;
    const update = await axios.post(commentUrl,{status:status, content:comment+"\n--\nscore:"+score});
    return update;
}
