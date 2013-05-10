package ca.gordonturner.bigboard.web.twitter;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

/**
 * @author gturner
 * 
 */
@Controller
public class TwitterTrendingTopics
{

  Logger logger = Logger.getLogger( TwitterTrendingTopics.class );


  private HashMap<String, Trends> trendsCollection;

  private HashMap<String, Calendar> lastUpdateCollections;

  // The Twitter API will throttle you for more then 150 hits / hour.
  private int refreshPeriodInMinutes = 15;

  private String mode;


  /*
   * 
   */
  public void init()
  {
    trendsCollection = new HashMap<String, Trends>();
    lastUpdateCollections = new HashMap<String, Calendar>();
  }


  /*
   * This method handles printing out the current contents of the tweets collection in memory.
   */
  @RequestMapping("/TwitterTrendingTopicsContents.html")
  @ResponseBody
  public String handleTwitterTrendingTopicsContents()
  {
    StringBuffer returnString = new StringBuffer();

    int i;

    for( String key : trendsCollection.keySet() )
    {

      returnString.append( "</br></br>" + key + "</br>" );

      i = 0;

      for( Trend trend : trendsCollection.get( key ).getTrends() )
      {
        i++;
        returnString.append( i + " " + trend.getName() + "</br>" );
      }
    }

    return returnString.toString();
  }


  /*
   * https://api.twitter.com/1/trends/daily.json
   */
  @RequestMapping("/TwitterTrendingTopics.html")
  public @ResponseBody
  HashMap<String, Trend[]> handleTwitterTrendingTopics( @RequestParam(value = "woeid", required = true)
  String woeid )
  {
    logger.debug( "Called" );

    if( "PROD".equals( mode ) )
    {
      return handleTwitterTrendingTopicsProd( woeid );
    }
    else
    {
      return handleTwitterTrendingTopicsTest( woeid );
    }
  }


  /**
   * @param screenName
   * @param stringCurrentTweetIndex
   * @return
   */
  private HashMap<String, Trend[]> handleTwitterTrendingTopicsProd( String woeid )
  {

    if( !trendsCollection.containsKey( woeid ) )
    {
      logger.info( "No cached trending tweets." );
      updateTwitterTrendingTopics( woeid );
    }
    else
    {
      GregorianCalendar now = new GregorianCalendar();
      GregorianCalendar lastUpdatePlusRefresh = (GregorianCalendar) lastUpdateCollections.get( woeid ).clone();
      lastUpdatePlusRefresh.add( GregorianCalendar.MINUTE, refreshPeriodInMinutes );

      if( now.after( lastUpdatePlusRefresh ) )
      {
        logger.info( "Cache has expired, calling twitter api." );
        updateTwitterTrendingTopics( woeid );
      }
      else
      {
        logger.info( "Cache is still valid." );
      }
    }

    HashMap<String, Trend[]> json = new HashMap<String, Trend[]>();

    json.put( "trends", trendsCollection.get( woeid ).getTrends() );
    return json;
  }


  /**
   * @param screenName
   * @param stringCurrentTweetIndex
   * @return
   */
  private HashMap<String, Trend[]> handleTwitterTrendingTopicsTest( String woeid )
  {


    HashMap<String, Trend[]> json = new HashMap<String, Trend[]>();
    json.put( "placeholder", null );
    return json;
  }


  /**
   * @param screenName
   * @param numberOfTweets
   */
  private void updateTwitterTrendingTopics( String woeid )
  {
    logger.info( "Updating twitter trending topics" );

    Twitter twitter = new TwitterFactory().getInstance();

    try
    {
      trendsCollection.put( woeid, twitter.getLocationTrends( Integer.parseInt( woeid ) ) );
    }
    catch( TwitterException e )
    {
      logger.error( "Error retrieving daily trendsList.", e );
    }

    lastUpdateCollections.put( woeid, new GregorianCalendar() );
  }


  /**
   * @param mode the mode to set
   */
  @Value("${TwitterTrendingTopics.mode}")
  public void setMode( String mode )
  {
    this.mode = mode;
  }
}
