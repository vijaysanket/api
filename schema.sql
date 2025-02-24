create table twitter_auth_asset(
    state varchar(200) primary key,
    codeChallenge varchar(200),
    codeVerifier varchar(200)
    status varchar(30)
 );